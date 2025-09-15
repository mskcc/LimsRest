package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.SearchQcResult;
import org.mskcc.limsrest.service.SearchQcResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for executing QC search operations.
 * Handles searching for QC data by PI name, type, recipe, or recent run folder patterns.
 */
public class GetSearchQcTask {
    private static final Log log = LogFactory.getLog(GetSearchQcTask.class);
    
    private static final int MAX_SMART_LIMIT = 1000;
    private static final int PAGINATION_BUFFER = 100;
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);

    private final String search;
    private final SearchField searchField;
    private final int limit;
    private final int offset;
    private final ConnectionLIMS conn;
    private Map<String, String> matchingRecipes;

    /**
     * Search field types for QC operations.
     */
    public enum SearchField {
        PI_NAME,
        TYPE,
        RECIPE,
        RECENT_RUN
    }

    /**
     * Constructs a new GetSearchQcTask with the specified search parameters.
     *
     * @param search the search term to filter by
     * @param searchField the field to search in
     * @param limit maximum number of results to return
     * @param offset number of results to skip for pagination
     * @param conn the LIMS database connection
     */
    public GetSearchQcTask(String search, SearchField searchField, int limit, int offset, ConnectionLIMS conn) {
        this.search = search;
        this.searchField = searchField;
        this.limit = limit;
        this.offset = offset;
        this.conn = conn;
    }

    /**
     * Executes the QC search operation with proper authorization.
     *
     * @return SearchQcResponse containing the search results and metadata
     */
    @PreAuthorize("hasRole('READ')")
    public SearchQcResponse execute() {
        long startTime = System.currentTimeMillis();
        
        try {
            Set<String> requestIds = new LinkedHashSet<>();
            performSearch(requestIds);

            List<SearchQcResult> allResults = buildResults(requestIds);
            sortResults(allResults);

            // Apply pagination
            int totalResults = allResults.size();
            int start = Math.min(offset, totalResults);
            int end = Math.min(start + limit, totalResults);
            List<SearchQcResult> paginatedResults = allResults.subList(start, end);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Search completed in " + totalTime + "ms: " + paginatedResults.size() + " returned, " + totalResults + " total");

            return SearchQcResponse.success(
                paginatedResults,
                totalResults,
                limit,
                offset,
                search,
                paginatedResults.size() == limit && end < totalResults
            );

        } catch (Exception e) {
            log.error("Error executing QC search for: '" + search + "' in field: " + searchField, e);
            return SearchQcResponse.error("Search failed: " + e.getMessage(), search, limit, offset);
        }
    }

    private VeloxConnection getFreshConnection() {
        return conn.getConnection();
    }

    /**
     * Performs the appropriate search based on the searchField type.
     * Delegates to specific search methods for each field type.
     *
     * @param requestIds Set to populate with matching request IDs
     * @throws Exception if search operation fails
     */
    private void performSearch(Set<String> requestIds) throws Exception {
        VeloxConnection vConn = getFreshConnection();
        User user = vConn.getUser();
        DataRecordManager drm = vConn.getDataRecordManager();
        
        // Decode URL-encoded characters (like %26 -> &)
        String decodedSearch = URLDecoder.decode(search, "UTF-8");
        log.info("Original search: '" + search + "' -> Decoded: '" + decodedSearch + "'");
        String sanitizedFilter = sanitizeForSql(decodedSearch);
        int smartLimit = Math.min(MAX_SMART_LIMIT, offset + limit + PAGINATION_BUFFER);

        // Initialize matchingRecipes for all search types
        matchingRecipes = new HashMap<>();

        switch (searchField) {
            case PI_NAME:
                searchByPiName(drm, user, requestIds, sanitizedFilter, smartLimit);
                break;
            case TYPE:
                searchByType(drm, user, requestIds, sanitizedFilter, smartLimit);
                break;
            case RECIPE:
                searchByRecipe(drm, user, requestIds, sanitizedFilter, smartLimit);
                break;
            case RECENT_RUN:
                searchByRecentRun(drm, user, requestIds, sanitizedFilter, smartLimit);
                break;
            default:
                throw new IllegalArgumentException("Unsupported search field: " + searchField);
        }
    }

    /**
     * Searches for requests by Principal Investigator (PI) name.
     *
     * @param drm DataRecordManager for database operations
     * @param user User performing the search
     * @param requestIds Set to populate with matching request IDs
     * @param sanitizedFilter SQL-safe search filter
     * @param smartLimit Maximum number of records to retrieve
     * @throws Exception if database query fails
     */
    private void searchByPiName(DataRecordManager drm, User user, Set<String> requestIds, 
                               String sanitizedFilter, int smartLimit) throws Exception {
        String query = "lower(LaboratoryHead) LIKE '%" + sanitizedFilter.toLowerCase() +
                       "%' ORDER BY DateCreated DESC LIMIT " + smartLimit;

        List<DataRecord> requests = drm.queryDataRecords("Request", query, user);
        List<Map<String, Object>> requestFields = drm.getFieldsForRecords(requests, user);

        for (Map<String, Object> fieldMap : requestFields) {
            String requestId = (String) fieldMap.get("RequestId");
            if (requestId != null && !requestId.trim().isEmpty()) {
                requestIds.add(requestId);
            }
        }
        
        log.debug("PI Name search for '" + sanitizedFilter + "' found " + requestIds.size() + " request IDs");
    }

    private void searchByType(DataRecordManager drm, User user, Set<String> requestIds, 
                             String sanitizedFilter, int smartLimit) throws Exception {
        String query = "lower(RequestName) LIKE '%" + sanitizedFilter.toLowerCase() +
                       "%' ORDER BY DateCreated DESC LIMIT " + smartLimit;

        List<DataRecord> requests = drm.queryDataRecords("Request", query, user);
        List<Map<String, Object>> requestFields = drm.getFieldsForRecords(requests, user);

        for (Map<String, Object> fieldMap : requestFields) {
            String requestId = (String) fieldMap.get("RequestId");
            if (requestId != null && !requestId.trim().isEmpty()) {
                requestIds.add(requestId);
            }
        }
        
        log.debug("Type search for '" + sanitizedFilter + "' found " + requestIds.size() + " request IDs");
    }

    /**
     * Searches for requests by recipe name.
     * Handles both exact matches (with underscore) and partial matches.
     *
     * @param drm DataRecordManager for database operations
     * @param user User performing the search
     * @param requestIds Set to populate with matching request IDs
     * @param sanitizedFilter SQL-safe search filter
     * @param smartLimit Maximum number of unique requests to retrieve
     * @throws Exception if database query fails
     */
    private void searchByRecipe(DataRecordManager drm, User user, Set<String> requestIds, 
                               String sanitizedFilter, int smartLimit) throws Exception {
        String lowerFilter = sanitizedFilter.toLowerCase();
        
        String recipeCondition = lowerFilter.contains("_") ? 
            "Recipe = '" + sanitizedFilter + "'" : 
            "Recipe LIKE '%" + sanitizedFilter + "%'";
            
        // Use higher limit for samples since multiple samples = 1 request
        int sampleLimit = Math.max(smartLimit * 15, 15000);
        
        String query = recipeCondition + " ORDER BY DateCreated DESC LIMIT " + sampleLimit;

        List<DataRecord> samples = drm.queryDataRecords("Sample", query, user);
        List<Map<String, Object>> sampleFields = drm.getFieldsForRecords(samples, user);

        int processedSamples = 0;
        for (Map<String, Object> fieldMap : sampleFields) {
            String requestId = (String) fieldMap.get("RequestId");
            String recipe = (String) fieldMap.get("Recipe");
            
            if (requestId != null && !requestId.trim().isEmpty()) {
                boolean isNewRequest = requestIds.add(requestId);
                
                if (isNewRequest && recipe != null) {
                    matchingRecipes.put(requestId, recipe);
                }
                
                if (requestIds.size() >= smartLimit) {
                    break;
                }
            }
            processedSamples++;
        }
        
        log.debug("Recipe search for '" + sanitizedFilter + "' found " + requestIds.size() + 
                 " unique request IDs from " + processedSamples + " samples");
    }

    private void searchByRecentRun(DataRecordManager drm, User user, Set<String> requestIds, 
                                  String sanitizedFilter, int smartLimit) throws Exception {
        // Search Request table for requests that have recent runs matching the pattern
        // This is more consistent than searching QC table directly
        String query = "RequestId IN (SELECT DISTINCT Request FROM SeqAnalysisSampleQC " +
                      "WHERE lower(SequencerRunFolder) LIKE '%" + sanitizedFilter.toLowerCase() + "%') " +
                      "ORDER BY DateCreated DESC LIMIT " + smartLimit;

        List<DataRecord> requests = drm.queryDataRecords("Request", query, user);
        List<Map<String, Object>> requestFields = drm.getFieldsForRecords(requests, user);

        for (Map<String, Object> fieldMap : requestFields) {
            String requestId = (String) fieldMap.get("RequestId");
            if (requestId != null && !requestId.trim().isEmpty()) {
                requestIds.add(requestId);
            }
        }
        
        log.debug("Recent Run search for '" + sanitizedFilter + "' found " + requestIds.size() + " request IDs");
    }

    /**
     * Builds complete SearchQcResult objects for all found request IDs.
     * Fetches request details, sample information, and QC data in bulk operations.
     *
     * @param requestIds Set of request IDs to process
     * @return List of SearchQcResult objects with complete information
     * @throws Exception if data retrieval fails
     */
    private List<SearchQcResult> buildResults(Set<String> requestIds) throws Exception {
        if (requestIds.isEmpty()) {
            return new ArrayList<>();
        }

        VeloxConnection vConn = getFreshConnection();
        User user = vConn.getUser();
        DataRecordManager drm = vConn.getDataRecordManager();

        // Process ALL request IDs to build complete result set before pagination
        List<String> requestIdsToProcess = new ArrayList<>(requestIds);

        // Single bulk query for requests
        String requestQuery = "RequestId IN (" + 
            requestIdsToProcess.stream()
                .map(id -> "'" + sanitizeForSql(id) + "'")
                .collect(Collectors.joining(",")) + ")";

        List<DataRecord> allRequests = drm.queryDataRecords("Request", requestQuery, user);

        // Bulk field operations
        List<Map<String, Object>> requestFields = drm.getFieldsForRecords(allRequests, user);
        List<List<Map<String, Object>>> childSampleFields = drm.getFieldsForChildrenOfType(allRequests, "Sample", user);

        // Bulk QC data fetch
        Map<String, QcData> qcDataMap = fetchQcData(drm, user, requestIdsToProcess);

        // Build results
        List<SearchQcResult> allResults = new ArrayList<>();
        for (int i = 0; i < requestFields.size(); i++) {
            try {
                Map<String, Object> requestFieldMap = requestFields.get(i);
                List<Map<String, Object>> sampleFieldMaps = childSampleFields.get(i);

                String requestId = (String) requestFieldMap.get("RequestId");
                String pi = (String) requestFieldMap.get("LaboratoryHead");
                String type = (String) requestFieldMap.get("RequestName");

                // Get recipe - for recipe searches, use the matching recipe; otherwise use first sample's recipe
                String recipe = null;
                if (searchField == SearchField.RECIPE && matchingRecipes != null && matchingRecipes.containsKey(requestId)) {
                    recipe = matchingRecipes.get(requestId);
                } else if (!sampleFieldMaps.isEmpty()) {
                    recipe = (String) sampleFieldMaps.get(0).get("Recipe");
                }

                String latestDate = "No QC data";
                String latestRecentRun = "No runs";

                if (qcDataMap.containsKey(requestId)) {
                    QcData qcData = qcDataMap.get(requestId);
                    if (qcData.dateCreated != null) {
                        // Convert Long timestamp to LocalDateTime and format
                        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(qcData.dateCreated / 1000, 0, 
                            java.time.ZoneOffset.UTC);
                        latestDate = dateTime.format(DATE_FORMATTER);
                    }
                    // Use the combined recent runs from QcData
                    if (qcData.recentRuns != null && !qcData.recentRuns.trim().isEmpty()) {
                        latestRecentRun = qcData.recentRuns;
                    }
                } else {
                    // Ensure we explicitly set the "no data" values
                    latestDate = "No QC data";
                    latestRecentRun = "No runs";
                }

                SearchQcResult result = new SearchQcResult();
                result.setPi(pi);
                result.setType(type);
                result.setRecipe(recipe);
                result.setRequestId(requestId);
                result.setRecentRuns(latestRecentRun);
                result.setDateOfLatestStats(latestDate);

                allResults.add(result);

            } catch (Exception e) {
                log.warn("Error processing request data for index " + i + ": " + e.getMessage());
            }
        }

        return allResults;
    }

    /**
     * Fetches QC data for multiple requests in a single bulk operation.
     * Groups QC records by request and extracts the most recent date and run folders.
     *
     * @param drm DataRecordManager for database operations
     * @param user User performing the search
     * @param requestIds List of request IDs to fetch QC data for
     * @return Map of request ID to QcData containing date and run information
     * @throws Exception if database query fails
     */
    private Map<String, QcData> fetchQcData(DataRecordManager drm, User user, List<String> requestIds) throws Exception {
        Map<String, QcData> qcDataMap = new HashMap<>();

        if (requestIds.isEmpty()) {
            return qcDataMap;
        }

        // Build QC query using the correct field names we discovered
        String qcQuery = "Request IN (" + 
            requestIds.stream()
                .map(id -> "'" + sanitizeForSql(id) + "'")
                .collect(Collectors.joining(",")) + ") ORDER BY DateCreated DESC";

        List<DataRecord> allQcRecords = drm.queryDataRecords("SeqAnalysisSampleQC", qcQuery, user);
        List<Map<String, Object>> qcFields = drm.getFieldsForRecords(allQcRecords, user);

        // Group QC records by request and collect recent runs
        Map<String, List<QcRecord>> qcByRequest = new HashMap<>();
        for (Map<String, Object> qcFieldMap : qcFields) {
            String request = (String) qcFieldMap.get("Request");
            String folder = (String) qcFieldMap.get("SequencerRunFolder");
            Long dateCreated = (Long) qcFieldMap.get("DateCreated");

            if (request != null && dateCreated != null) {
                boolean shouldInclude = searchField != SearchField.RECENT_RUN ||
                    (folder != null && folder.toLowerCase().contains(search.toLowerCase()));

                if (shouldInclude) {
                    qcByRequest.computeIfAbsent(request, k -> new ArrayList<>())
                              .add(new QcRecord(dateCreated, folder));
                }
            }
        }

        // Process each request's QC data
        for (Map.Entry<String, List<QcRecord>> entry : qcByRequest.entrySet()) {
            String requestId = entry.getKey();
            List<QcRecord> qcRecords = entry.getValue();
            
            if (!qcRecords.isEmpty()) {
                // Sort by date descending to get most recent first
                qcRecords.sort((a, b) -> Long.compare(b.dateCreated, a.dateCreated));
                
                // Get most recent date
                Long mostRecentDate = qcRecords.get(0).dateCreated;
                
                // Collect unique recent runs (extract run prefix like FAUCI_0268)
                Set<String> recentRunPrefixes = new LinkedHashSet<>();
                for (QcRecord qcRecord : qcRecords) {
                    if (qcRecord.runFolder != null && qcRecord.runFolder.contains("_")) {
                        String[] parts = qcRecord.runFolder.split("_");
                        if (parts.length >= 2) {
                            recentRunPrefixes.add(parts[0] + "_" + parts[1]);
                        }
                    }
                }
                
                // Combine recent runs with comma separator
                String combinedRecentRuns = recentRunPrefixes.isEmpty() ? 
                    "No runs" : String.join(", ", recentRunPrefixes);
                
                qcDataMap.put(requestId, new QcData(mostRecentDate, combinedRecentRuns));
            }
        }

        return qcDataMap;
    }
    
    /**
     * Sorts results by QC date in descending order.
     * Results with no QC data are placed at the end of the list.
     *
     * @param allResults List of results to sort in-place
     */
    private void sortResults(List<SearchQcResult> allResults) {
        allResults.sort((a, b) -> {
            String dateStringA = a.getDateOfLatestStats();
            String dateStringB = b.getDateOfLatestStats();

            // Handle all "no data" cases - null, empty string, or "No QC data"
            boolean aHasNoData = (dateStringA == null || dateStringA.trim().isEmpty() || "No QC data".equals(dateStringA));
            boolean bHasNoData = (dateStringB == null || dateStringB.trim().isEmpty() || "No QC data".equals(dateStringB));
            
            // Both have no data - maintain their current relative order
            if (aHasNoData && bHasNoData) return 0;
            
            // Only a has no data - push a to the end
            if (aHasNoData) return 1;
            
            // Only b has no data - push b to the end  
            if (bHasNoData) return -1;

            // Both have QC data - sort by date descending (most recent first)
            LocalDateTime dateA = parseDate(dateStringA);
            LocalDateTime dateB = parseDate(dateStringB);

            if (dateA != null && dateB != null) {
                return dateB.compareTo(dateA); // Descending order (newest first)
            }

            // If one date failed to parse, treat it as "no data"
            if (dateA == null && dateB != null) return 1;  // a to end
            if (dateB == null && dateA != null) return -1; // b to end

            // Both failed to parse - fallback to string comparison
            return dateStringB.compareTo(dateStringA);
        });
    }

    /**
     * Parses a date string into LocalDateTime using the standard format.
     *
     * @param dateString The date string to parse
     * @return LocalDateTime object or null if parsing fails
     */
    private LocalDateTime parseDate(String dateString) {
        if (dateString == null || "No QC data".equals(dateString)) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.debug("Failed to parse date string: " + dateString, e);
            return null;
        }
    }

    /**
     * Sanitizes a string for safe use in SQL queries by escaping single quotes.
     *
     * @param input The input string to sanitize
     * @return The sanitized string with escaped quotes
     */
    private String sanitizeForSql(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("'", "''");
    }

    /**
     * Container class for QC data associated with a request.
     */
    private static class QcData {
        final Long dateCreated;
        final String recentRuns;

        /**
         * Creates a new QcData instance.
         *
         * @param dateCreated Timestamp of the most recent QC record
         * @param recentRuns Comma-separated list of recent run prefixes
         */
        QcData(Long dateCreated, String recentRuns) {
            this.dateCreated = dateCreated;
            this.recentRuns = recentRuns;
        }
    }

    /**
     * Container class for individual QC record data.
     */
    private static class QcRecord {
        final Long dateCreated;
        final String runFolder;

        /**
         * Creates a new QcRecord instance.
         *
         * @param dateCreated Timestamp when the QC record was created
         * @param runFolder The sequencer run folder name
         */
        QcRecord(Long dateCreated, String runFolder) {
            this.dateCreated = dateCreated;
            this.runFolder = runFolder;
        }
    }
}