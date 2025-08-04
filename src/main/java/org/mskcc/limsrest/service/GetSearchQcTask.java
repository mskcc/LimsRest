
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

import java.util.*;
import java.util.regex.Pattern;

/**
 * A queued task that performs intelligent QC search with pattern detection.
 * Supports PI name, recipe, and recent run searches.
 */
public class GetSearchQcTask {
    private static Log log = LogFactory.getLog(GetSearchQcTask.class);
    
    private final String search;
    private final int limit;
    private final int offset;
    private final ConnectionLIMS conn;
    
    private static final Pattern RECENT_RUN_PATTERN = Pattern.compile("^[A-Za-z]+_[0-9]+.*$");

    public GetSearchQcTask(String search, int limit, int offset, ConnectionLIMS conn) {
        this.search = search;
        this.limit = limit;
        this.offset = offset;
        this.conn = conn;
    }
    
    @PreAuthorize("hasRole('READ')")
    public SearchQcResponse execute() {
        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager drm = vConn.getDataRecordManager();

        try {
            Set<String> requestIds = new LinkedHashSet<>();
            
            SearchType searchType = detectSearchType(search);
            log.info("Detected search type: " + searchType + " for search: '" + search + "'");

            switch (searchType) {
                case PI_NAME:
                    searchByPiName(drm, user, search, requestIds);
                    break;
                case RECENT_RUN:
                    searchByRecentRun(drm, user, search, requestIds);
                    break;
                case RECIPE:
                    searchByRecipe(drm, user, search, requestIds);
                    break;
            }

            log.info("Total unique request IDs found: " + requestIds.size());

            List<SearchQcResult> allResults = buildSearchResults(drm, user, requestIds, searchType);
            
            allResults.sort((a, b) -> {
                String dateA = a.getDateOfLatestStats();
                String dateB = b.getDateOfLatestStats();
                
                if (dateA == null && dateB == null) return 0;
                if (dateA == null) return 1;
                if (dateB == null) return -1;
                return dateB.compareTo(dateA);
            });

            int totalResults = allResults.size();
            int start = Math.min(offset, totalResults);
            int end = Math.min(start + limit, totalResults);
            
            List<SearchQcResult> paginatedResults = allResults.subList(start, end);
            
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
    
    private SearchType detectSearchType(String searchTerm) {
        String trimmed = searchTerm.trim();
        
        if (RECENT_RUN_PATTERN.matcher(trimmed).matches()) {
            return SearchType.RECENT_RUN;
        }
        
        if (trimmed.contains("*")) {
            return SearchType.RECIPE;
        }
        
        return SearchType.PI_NAME;
    }
    
    private void searchByPiName(DataRecordManager drm, User user, String piName, Set<String> requestIds) throws Exception {
        long start = System.currentTimeMillis();
        String sanitizedFilter = piName.replace("'", "''");
        String query = "lower(Investigator) LIKE '%" + sanitizedFilter.toLowerCase() + "%' ORDER BY DateCreated DESC LIMIT 60";
        List<DataRecord> requests = drm.queryDataRecords("Request", query, user);
        
        for (DataRecord r : requests) {
            String requestId = (String) r.getValue("RequestId", user);
            if (requestId != null && !requestId.trim().isEmpty()) {
                requestIds.add(requestId);
            }
        }
        log.info("PI search for '" + piName + "' found " + requests.size() + " requests in " + 
                (System.currentTimeMillis() - start) + "ms");
    }
    
    private void searchByRecentRun(DataRecordManager drm, User user, String runName, Set<String> requestIds) throws Exception {
        long start = System.currentTimeMillis();
        String sanitizedFilter = runName.replace("'", "''");
        String query = "lower(SequencerRunFolder) LIKE '%" + sanitizedFilter.toLowerCase() + "%' ORDER BY DateCreated DESC LIMIT 60";
        List<DataRecord> saqRecords = drm.queryDataRecords("SeqAnalysisSampleQC", query, user);
        
        for (DataRecord saq : saqRecords) {
            List<DataRecord> parents = saq.getParentsOfType("Sample", user);
            if (!parents.isEmpty()) {
                DataRecord sample = parents.get(0);
                String requestId = (String) sample.getValue("RequestId", user);
                if (requestId != null && !requestId.trim().isEmpty()) {
                    requestIds.add(requestId);
                }
            }
        }
        log.info("Recent run search for '" + runName + "' found " + saqRecords.size() + " SAQ records in " + 
                (System.currentTimeMillis() - start) + "ms");
    }
    
    private void searchByRecipe(DataRecordManager drm, User user, String recipe, Set<String> requestIds) throws Exception {
        long start = System.currentTimeMillis();
        String cleanRecipe = recipe.replace("*", "").replace("'", "''");
        String query = "lower(Recipe) LIKE '%" + cleanRecipe.toLowerCase() + "%' ORDER BY DateCreated DESC LIMIT 60";
        List<DataRecord> samples = drm.queryDataRecords("Sample", query, user);
        
        for (DataRecord s : samples) {
            String requestId = (String) s.getValue("RequestId", user);
            if (requestId != null && !requestId.trim().isEmpty()) {
                requestIds.add(requestId);
            }
        }
        log.info("Recipe search for '" + recipe + "' found " + samples.size() + " samples in " + 
                (System.currentTimeMillis() - start) + "ms");
    }
    
    private List<SearchQcResult> buildSearchResults(DataRecordManager drm, User user, Set<String> requestIds, SearchType searchType) throws Exception {
        long summaryStart = System.currentTimeMillis();
        List<SearchQcResult> allResults = new ArrayList<>();
        
        int processedCount = 0;
        int maxToProcess = Math.min(requestIds.size(), 50);

        for (String requestId : requestIds) {
            if (processedCount >= maxToProcess) {
                log.info("Reached processing limit of " + maxToProcess + " projects for performance");
                break;
            }
            
            try {
                List<DataRecord> reqs = drm.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
                if (reqs.isEmpty()) continue;
                
                DataRecord req = reqs.get(0);
                String pi = (String) req.getValue("Investigator", user);
                String type = (String) req.getValue("RequestName", user);

                String recipe = null;
                DataRecord[] childSamples = req.getChildrenOfType("Sample", user);
                if (childSamples.length > 0) {
                    recipe = (String) childSamples[0].getValue("Recipe", user);
                }

                String latestDate = "No QC data";
                String latestRecentRun = "No runs";
                
                if (childSamples.length > 0) {
                    try {
                        List<DataRecord> saqList = childSamples[0].getDescendantsOfType("SeqAnalysisSampleQC", user);
                        if (!saqList.isEmpty()) {
                            
                            if (searchType == SearchType.RECENT_RUN) {
                                DataRecord matchingQC = null;
                                for (DataRecord saq : saqList) {
                                    String folder = (String) saq.getValue("SequencerRunFolder", user);
                                    if (folder != null && folder.toLowerCase().contains(search.toLowerCase())) {
                                        matchingQC = saq;
                                        break;
                                    }
                                }
                                
                                if (matchingQC != null) {
                                    Long dateCreated = (Long) matchingQC.getValue("DateCreated", user);
                                    if (dateCreated != null) {
                                        latestDate = new Date(dateCreated).toString();
                                    }
                                    
                                    String folder = (String) matchingQC.getValue("SequencerRunFolder", user);
                                    if (folder != null && folder.contains("_")) {
                                        String[] parts = folder.split("_");
                                        if (parts.length >= 2) {
                                            latestRecentRun = parts[0] + "_" + parts[1];
                                        }
                                    }
                                } else {
                                    continue;
                                }
                            } else {
                                DataRecord mostRecent = saqList.get(saqList.size() - 1);
                                Long dateCreated = (Long) mostRecent.getValue("DateCreated", user);
                                if (dateCreated != null) {
                                    latestDate = new Date(dateCreated).toString();
                                }
                                
                                String folder = (String) mostRecent.getValue("SequencerRunFolder", user);
                                if (folder != null && folder.contains("_")) {
                                    String[] parts = folder.split("_");
                                    if (parts.length >= 2) {
                                        latestRecentRun = parts[0] + "_" + parts[1];
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Could not retrieve QC data for request: " + requestId);
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
                processedCount++;
                
            } catch (Exception e) {
                log.warn("Error processing request " + requestId + ": " + e.getMessage());
            }
        }
        
        log.info("Built " + allResults.size() + " summaries from " + processedCount + " processed projects in " + 
                (System.currentTimeMillis() - summaryStart) + "ms");
        return allResults;
    }
    
    private enum SearchType {
        PI_NAME,
        RECENT_RUN,
        RECIPE
    }
}

