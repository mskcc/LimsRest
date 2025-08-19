package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.SearchQcResult;
import org.springframework.security.access.prepost.PreAuthorize;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service class for executing QC search operations.
 * Handles searching for QC data by PI name or recent run folder patterns.
 * Automatically detects search type and routes to appropriate database queries.
 */
public class GetSearchQcTask {
    private static final Log log = LogFactory.getLog(GetSearchQcTask.class);
    private static final Pattern RECENT_RUN_PATTERN = Pattern.compile("^[A-Za-z0-9]+_[0-9]+.*$");
    
    // Constants for better maintainability
    private static final int MAX_SMART_LIMIT = 800;
    private static final int PAGINATION_BUFFER = 100;
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm";

    private final String search;
    private final int limit;
    private final int offset;
    private final ConnectionLIMS conn;

    /**
     * Constructs a new GetSearchQcTask with the specified search parameters.
     *
     * @param search the search term to filter by (PI name or run folder pattern)
     * @param limit maximum number of results to return
     * @param offset number of results to skip for pagination
     * @param conn the LIMS database connection
     */
    public GetSearchQcTask(String search, int limit, int offset, ConnectionLIMS conn) {
        this.search = search;
        this.limit = limit;
        this.offset = offset;
        this.conn = conn;
    }

    /**
     * Executes the QC search operation with proper authorization.
     * Automatically detects search type, performs the search, builds results, and applies pagination.
     *
     * @return SearchQcResponse containing the search results and metadata
     */
    @PreAuthorize("hasRole('READ')")
    public SearchQcResponse execute() {
        long startTime = System.currentTimeMillis();
        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager drm = vConn.getDataRecordManager();

        try {
            Set<String> requestIds = new LinkedHashSet<>();
            SearchType searchType = performSearch(drm, user, requestIds);

            List<SearchQcResult> allResults = buildResults(drm, user, requestIds, searchType);
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
            log.error("Error executing QC search for: '" + search + "'", e);
            return SearchQcResponse.error("Search failed: " + e.getMessage(), search, limit, offset);
        }
    }

    /**
     * Performs the initial search operation to collect request IDs based on search type.
     * Automatically detects whether to search by PI name or recent run folder and executes the appropriate query.
     *
     * @param drm the data record manager for database operations
     * @param user the authenticated user performing the search
     * @param requestIds the set to populate with matching request IDs
     * @return the detected search type (PI_NAME or RECENT_RUN)
     * @throws Exception if database operations fail
     */
   private SearchType performSearch(DataRecordManager drm, User user, Set<String> requestIds) throws Exception {
        String sanitizedFilter = search.replace("'", "''");
        SearchType searchType = detectSearchType();

        // Only fetch what we need for pagination (+ some headroom)
        int smartLimit = Math.min(MAX_SMART_LIMIT, offset + limit + PAGINATION_BUFFER);

        if (searchType == SearchType.RECENT_RUN) {
            // Find QC rows whose run folder matches; then backtrack to Request IDs
            String qcQuery = "lower(SequencerRunFolder) LIKE '%" + sanitizedFilter.toLowerCase() +
                             "%' ORDER BY DateCreated DESC LIMIT " + smartLimit;

            List<DataRecord> saqRecords = drm.queryDataRecords("SeqAnalysisSampleQC", qcQuery, user);

            // Pull Request IDs directly from QC records
            List<Map<String, Object>> qcFields = drm.getFieldsForRecords(saqRecords, user);
            for (Map<String, Object> qcFieldMap : qcFields) {
                String requestId = (String) qcFieldMap.get("Request"); // field name in QC table
                if (requestId != null && !requestId.trim().isEmpty()) {
                    requestIds.add(requestId);
                }
            }

            // Also try parent Sample → RequestId (defensive, in case some QC rows lack the Request field)
            List<List<DataRecord>> parentSamples = drm.getParentsOfType(saqRecords, "Sample", user);
            for (List<DataRecord> parents : parentSamples) {
                if (!parents.isEmpty()) {
                    try {
                        String requestId = (String) parents.get(0).getValue("RequestId", user);
                        if (requestId != null && !requestId.trim().isEmpty()) {
                            requestIds.add(requestId);
                        }
                    } catch (Exception ignore) {
                        
                    }
                }
            }
        } else {
            // PI name search on Request table
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
        }

        return searchType;
    }

    /**
     * Builds the complete search results by fetching detailed information for each request ID.
     * Performs bulk operations to efficiently retrieve request details and QC data.
     *
     * @param drm the data record manager for database operations
     * @param user the authenticated user performing the search
     * @param requestIds the set of request IDs to build results for
     * @param searchType the type of search being performed
     * @return list of SearchQcResult objects with populated data
     * @throws Exception if database operations fail
     */
    private List<SearchQcResult> buildResults(DataRecordManager drm, User user, Set<String> requestIds, SearchType searchType) throws Exception {
        if (requestIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Only process what we need for pagination
        int maxToProcess = Math.min(requestIds.size(), offset + limit + PAGINATION_BUFFER);
        List<String> requestIdsToProcess = new ArrayList<>(requestIds).subList(0, maxToProcess);

        // Single bulk query for requests - improved SQL injection protection
        String requestQuery = "RequestId IN (" + 
            requestIdsToProcess.stream()
                .map(id -> "'" + sanitizeForSql(id) + "'")  // Better sanitization
                .collect(Collectors.joining(",")) + ")";

        List<DataRecord> allRequests = drm.queryDataRecords("Request", requestQuery, user);

        // Bulk field operations
        List<Map<String, Object>> requestFields = drm.getFieldsForRecords(allRequests, user);
        List<List<Map<String, Object>>> childSampleFields = drm.getFieldsForChildrenOfType(allRequests, "Sample", user);

        // Bulk QC data fetch
        Map<String, QcData> qcDataMap = fetchQcData(drm, user, requestIdsToProcess, searchType);

        // Build results
        List<SearchQcResult> allResults = new ArrayList<>();
        for (int i = 0; i < requestFields.size(); i++) {
            try {
                Map<String, Object> requestFieldMap = requestFields.get(i);
                List<Map<String, Object>> sampleFieldMaps = childSampleFields.get(i);

                String requestId = (String) requestFieldMap.get("RequestId");
                String pi = (String) requestFieldMap.get("LaboratoryHead");
                String type = (String) requestFieldMap.get("RequestName");

                String recipe = null;
                if (!sampleFieldMaps.isEmpty()) {
                    recipe = (String) sampleFieldMaps.get(0).get("Recipe");
                }

                String latestDate = "No QC data";
                String latestRecentRun = "No runs";

                if (qcDataMap.containsKey(requestId)) {
                    QcData qcData = qcDataMap.get(requestId);
                    if (qcData.dateCreated != null) {
                        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT_PATTERN);
                        latestDate = formatter.format(new Date(qcData.dateCreated));
                    }
                    if (qcData.runFolder != null && qcData.runFolder.contains("_")) {
                        String[] parts = qcData.runFolder.split("_");
                        if (parts.length >= 2) {
                            latestRecentRun = parts[0] + "_" + parts[1];
                        }
                    }
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
     * Fetches QC data for the specified request IDs.
     * Retrieves SeqAnalysisSampleQC records and maps them to request IDs with filtering based on search type.
     *
     * @param drm the data record manager for database operations
     * @param user the authenticated user performing the search
     * @param requestIds list of request IDs to fetch QC data for
     * @param searchType the type of search being performed (affects filtering)
     * @return map of request IDs to their corresponding QC data
     * @throws Exception if database operations fail
     */
    private Map<String, QcData> fetchQcData(DataRecordManager drm, User user, List<String> requestIds, SearchType searchType) throws Exception {
        Map<String, QcData> qcDataMap = new HashMap<>();

        String qcQuery = "Request IN (" + 
            requestIds.stream()
                .map(id -> "'" + sanitizeForSql(id) + "'")  // Better sanitization
                .collect(Collectors.joining(",")) + ")";

        if (searchType == SearchType.RECENT_RUN) {
            qcQuery += " AND lower(SequencerRunFolder) LIKE '%" + sanitizeForSql(search.toLowerCase()) + "%'";
        }
        qcQuery += " ORDER BY DateCreated DESC";

        List<DataRecord> allQcRecords = drm.queryDataRecords("SeqAnalysisSampleQC", qcQuery, user);
        List<Map<String, Object>> qcFields = drm.getFieldsForRecords(allQcRecords, user);

        // For PI_NAME: keep most recent QC per request
        // For RECENT_RUN: keep matching QC records
        for (Map<String, Object> qcFieldMap : qcFields) {
            String request = (String) qcFieldMap.get("Request");
            String folder = (String) qcFieldMap.get("SequencerRunFolder");
            Long dateCreated = (Long) qcFieldMap.get("DateCreated");

            if (request != null && dateCreated != null) {
                boolean shouldInclude = searchType == SearchType.PI_NAME ||
                    (folder != null && folder.toLowerCase().contains(search.toLowerCase()));

                if (shouldInclude && !qcDataMap.containsKey(request)) {
                    qcDataMap.put(request, new QcData(dateCreated, folder));
                }
            }
        }

        return qcDataMap;
    }

    /**
     * Sorts the search results by date of latest stats.
     * Results with current year QC data are prioritized, followed by other results in descending date order.
     *
     * @param allResults the list of search results to sort in-place
     */
    private void sortResults(List<SearchQcResult> allResults) {
        allResults.sort((a, b) -> {
            String dateStringA = a.getDateOfLatestStats();
            String dateStringB = b.getDateOfLatestStats();

            if ((dateStringA == null || "No QC data".equals(dateStringA)) && 
                (dateStringB == null || "No QC data".equals(dateStringB))) return 0;
            if (dateStringA == null || "No QC data".equals(dateStringA)) return 1;
            if (dateStringB == null || "No QC data".equals(dateStringB)) return -1;

            Date dateA = parseDate(dateStringA);
            Date dateB = parseDate(dateStringB);

            if (dateA != null && dateB != null) {
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);

                Calendar calA = Calendar.getInstance();
                Calendar calB = Calendar.getInstance();
                calA.setTime(dateA);
                calB.setTime(dateB);
                int yearA = calA.get(Calendar.YEAR);
                int yearB = calB.get(Calendar.YEAR);

                if (yearA == currentYear && yearB != currentYear) return -1;
                if (yearB == currentYear && yearA != currentYear) return 1;

                return dateB.compareTo(dateA);
            }

            return dateStringB.compareTo(dateStringA);
        });
    }

    /**
     * Detects the type of search based on the search pattern.
     * Uses regex pattern matching to determine if search is for a recent run folder or PI name.
     *
     * @return SearchType.RECENT_RUN if search matches run folder pattern, SearchType.PI_NAME otherwise
     */
    private SearchType detectSearchType() {
        return RECENT_RUN_PATTERN.matcher(search.trim()).matches() ? 
               SearchType.RECENT_RUN : SearchType.PI_NAME;
    }

    /**
     * Parses a date string into a Date object.
     * Handles the date format used by the system (yyyy-MM-dd HH:mm) and returns null for invalid dates.
     *
     * @param dateString the date string to parse in format "yyyy-MM-dd HH:mm"
     * @return parsed Date object or null if parsing fails or string is invalid
     */
    private Date parseDate(String dateString) {
        if (dateString == null || "No QC data".equals(dateString)) {
            return null;
        }

        try {
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT_PATTERN);
            return formatter.parse(dateString);
        } catch (Exception e) {
            log.debug("Failed to parse date string: " + dateString, e);
            return null;
        }
    }

    /**
     * Sanitizes a string for safe SQL usage by escaping single quotes.
     * 
     * @param input the input string to sanitize
     * @return sanitized string with escaped single quotes
     */
    private String sanitizeForSql(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("'", "''");
    }

    /**
     * Internal data class for holding QC-related information.
     * Contains the creation date and run folder information for QC records.
     */
    private static class QcData {
        final Long dateCreated;
        final String runFolder;

        /**
         * Constructs QcData with the specified creation date and run folder.
         *
         * @param dateCreated the timestamp when the QC record was created
         * @param runFolder the sequencer run folder name
         */
        QcData(Long dateCreated, String runFolder) {
            this.dateCreated = dateCreated;
            this.runFolder = runFolder;
        }
    }

    /**
     * Enumeration defining the types of search operations supported.
     */
    private enum SearchType {
        /** Search by Principal Investigator name */
        PI_NAME, 
        /** Search by recent run folder pattern */
        RECENT_RUN
    }
}