package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.SearchQcTask;
import org.mskcc.limsrest.service.SearchQcResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class SearchQcController {
    private static Log log = LogFactory.getLog(SearchQcController.class);
    private final ConnectionLIMS conn;

    public SearchQcController(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/searchQc")
    public SearchQcResponse getContent(
            @RequestParam(value = "search", required = true) String search,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
            HttpServletRequest request
    ) {
        log.info("Starting /searchQc for search: '" + search + "' (limit: " + limit + ", offset: " + offset + ") client IP: " + request.getRemoteAddr());
        
        // Basic validation following your codebase pattern
        if (search == null || search.trim().isEmpty()) {
            log.error("FAILURE: search parameter is required and cannot be empty.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: search parameter is required and cannot be empty.");
        }

        // Validate and enforce limits for performance with large datasets
        int validatedLimit = Math.min(Math.max(limit, 1), 50); // Between 1 and 50
        int validatedOffset = Math.min(Math.max(offset, 0), 200); // Between 0 and 200
        
        if (limit > 50) {
            log.warn("Requested limit " + limit + " exceeds maximum 50, using 50");
        }
        if (offset > 200) {
            log.warn("Requested offset " + offset + " exceeds maximum 200, using 200");
        }

        try {
            long startTime = System.currentTimeMillis();
            SearchQcTask task = new SearchQcTask(search.trim(), validatedLimit, validatedOffset, conn);
            SearchQcResponse response = task.execute();
            long endTime = System.currentTimeMillis();
            
            log.info("QC search completed in " + (endTime - startTime) + "ms: " + 
                    response.getResults().size() + " results returned (total found: " + response.getTotal() + ")");
            return response;
            
        } catch (Exception e) {
            log.error("Error executing QC search for: '" + search + "'", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Search failed: " + e.getMessage());
        }
    }
}