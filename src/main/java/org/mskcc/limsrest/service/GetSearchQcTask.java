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

public class GetSearchQcTask {
    private static final Log log = LogFactory.getLog(GetSearchQcTask.class);
    private static final Pattern RECENT_RUN_PATTERN = Pattern.compile("^[A-Za-z0-9]+_[0-9]+.*$");
    
    private final String search;
    private final int limit;
    private final int offset;
    private final ConnectionLIMS conn;

    public GetSearchQcTask(String search, int limit, int offset, ConnectionLIMS conn) {
        this.search = search;
        this.limit = limit;
        this.offset = offset;
        this.conn = conn;
    }
    
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
    
    private SearchType performSearch(DataRecordManager drm, User user, Set<String> requestIds) throws Exception {
        String sanitizedFilter = search.replace("'", "''");
        SearchType searchType = detectSearchType();
        
        // Calculate smart limit - only fetch what we need for pagination
        int smartLimit = Math.min(800, offset + limit + 100);
        
        if (searchType == SearchType.RECENT_RUN) {
            String query = "lower(SequencerRunFolder) LIKE '%" + sanitizedFilter.toLowerCase() + 
                          "%' ORDER BY DateCreated DESC LIMIT " + smartLimit;
            
            List<DataRecord> saqRecords = drm.queryDataRecords("SeqAnalysisSampleQC", query, user);
            List<List<DataRecord>> parentSamples = drm.getParentsOfType(saqRecords, "Sample", user);
            
            for (List<DataRecord> parents : parentSamples) {
                if (!parents.isEmpty()) {
                    try {
                        String requestId = (String) parents.get(0).getValue("RequestId", user);
                        if (requestId != null && !requestId.trim().isEmpty()) {
                            requestIds.add(requestId);
                        }
                    } catch (Exception e) {
                    
                    }
                }
            }
        } else {
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
    
    private List<SearchQcResult> buildResults(DataRecordManager drm, User user, Set<String> requestIds, SearchType searchType) throws Exception {
        if (requestIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Only process what we need for pagination
        int maxToProcess = Math.min(requestIds.size(), offset + limit + 100);
        List<String> requestIdsToProcess = new ArrayList<>(requestIds).subList(0, maxToProcess);
        
        // Single bulk query for requests
        String requestQuery = "RequestId IN (" + 
            requestIdsToProcess.stream()
                .map(id -> "'" + id + "'")
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
                        latestDate = new Date(qcData.dateCreated).toString();
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
                
            }
        }
        
        return allResults;
    }
    
    private Map<String, QcData> fetchQcData(DataRecordManager drm, User user, List<String> requestIds, SearchType searchType) throws Exception {
        Map<String, QcData> qcDataMap = new HashMap<>();
        
        String qcQuery = "Request IN (" + 
            requestIds.stream()
                .map(id -> "'" + id + "'")
                .collect(Collectors.joining(",")) + ")";
        
        if (searchType == SearchType.RECENT_RUN) {
            qcQuery += " AND lower(SequencerRunFolder) LIKE '%" + search.toLowerCase() + "%'";
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
    
    private SearchType detectSearchType() {
        return RECENT_RUN_PATTERN.matcher(search.trim()).matches() ? 
               SearchType.RECENT_RUN : SearchType.PI_NAME;
    }
    
    private Date parseDate(String dateString) {
        if (dateString == null || "No QC data".equals(dateString)) {
            return null;
        }
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
            return formatter.parse(dateString);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static class QcData {
        final Long dateCreated;
        final String runFolder;
        
        QcData(Long dateCreated, String runFolder) {
            this.dateCreated = dateCreated;
            this.runFolder = runFolder;
        }
    }
    
    private enum SearchType {
        PI_NAME, RECENT_RUN
    }
}