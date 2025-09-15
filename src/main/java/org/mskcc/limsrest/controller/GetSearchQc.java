package org.mskcc.limsrest.controller;
 
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetSearchQcTask;
import org.mskcc.limsrest.service.GetSearchQcTask.SearchField;
import org.mskcc.limsrest.service.SearchQcResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
 
import javax.servlet.http.HttpServletRequest;
 
/**
* REST controller for QC search operations.
* Provides endpoints for searching quality control data by a single field at a time:
* PI name, recipe, type, or sequencer run folders.
*/
@RestController
@RequestMapping("/")
public class GetSearchQc {
    private static Log log = LogFactory.getLog(GetSearchQc.class);
    private final ConnectionLIMS conn;
 
    /**
     * Constructs the controller with LIMS connection dependency.
     *
     * @param conn the LIMS database connection
     */
    public GetSearchQc(ConnectionLIMS conn) {
        this.conn = conn;
    }
 
    /**
     * Handles QC search requests with single API format support.
     * Only supports: /searchQc?searchField={fieldType}&search={searchTerm}
     *
     * @param search the search term to filter by (required)
     * @param searchField the field to search in (required) - "PI Name", "Recipes"/"Recipe", "Type", "Recent Run"
     * @param limit maximum number of results to return (default: 100)
     * @param offset number of results to skip for pagination (default: 0)
     * @param request HTTP request object for logging client information
     * @return SearchQcResponse containing search results and pagination metadata
     * @throws ResponseStatusException if search parameters are invalid or search fails
     */
    @GetMapping("/searchQc")
    public SearchQcResponse getContent(
            @RequestParam("search") String search,
            @RequestParam("searchField") String searchField,
            @RequestParam(value = "limit", defaultValue = "100") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            HttpServletRequest request
    ) {
        // Validate required parameters
        if (search == null || search.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search term is required");
        }
        
        if (searchField == null || searchField.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SearchField is required");
        }
 
       
        SearchField field = mapSearchField(searchField.trim());
        if (field == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid searchField. Supported values: 'PI Name', 'Recipe', 'Type', 'Recent Run'");
        }
 
        log.info("Starting /searchQc for " + searchField + "='" + search + "' (limit: " + limit +
                ", offset: " + offset + ") client IP: " + request.getRemoteAddr());
 
        int validatedLimit = Math.max(limit, 1);
        int validatedOffset = Math.max(offset, 0);
 
        try {
            long startTime = System.currentTimeMillis();
            
            GetSearchQcTask task = new GetSearchQcTask(
                search.trim(),
                field,
                validatedLimit,
                validatedOffset,
                conn
            );
            
            SearchQcResponse response = task.execute();
            long endTime = System.currentTimeMillis();
 
            log.info("QC search completed in " + (endTime - startTime) + "ms: " +
                    response.getResults().size() + " results returned (total found: " + response.getTotal() + ")");
            return response;
 
        } catch (Exception e) {
            log.error("Error executing QC search for: " + searchField + "='" + search + "'", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Search failed: " + e.getMessage());
        }
    }
 
    /**
     * Maps frontend searchField values to internal SearchField enum.
     *
     * @param searchField the search field name from the request
     * @return corresponding SearchField enum value, or null if not recognized
     */
    private SearchField mapSearchField(String searchField) {
        switch (searchField) {
            case "PI Name":
                return SearchField.PI_NAME;  
            case "Recipe":
                return SearchField.RECIPE;
            case "Type":
                return SearchField.TYPE;
            case "Recent Run":
                return SearchField.RECENT_RUN;
            default:
                return null;
        }
    }
} 
 