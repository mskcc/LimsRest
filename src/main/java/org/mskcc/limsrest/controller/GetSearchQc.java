package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetSearchQcTask;
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
public class GetSearchQc {
    private static Log log = LogFactory.getLog(GetSearchQc.class);
    private final ConnectionLIMS conn;

    public GetSearchQc(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/searchQc")
    public SearchQcResponse getContent(
            @RequestParam(value = "search", required = true) String search,
            @RequestParam(value = "limit", required = false, defaultValue = "100") int limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
            HttpServletRequest request
    ) {
        log.info("Starting /searchQc for search: '" + search + "' (limit: " + limit + ", offset: " + offset + ") client IP: " + request.getRemoteAddr());

        if (search == null || search.trim().isEmpty()) {
            log.error("FAILURE: search parameter is required and cannot be empty.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: search parameter is required and cannot be empty.");
        }
        
        int validatedLimit = Math.max(limit, 1);
        int validatedOffset = Math.max(offset, 0);

        try {
            long startTime = System.currentTimeMillis();
            GetSearchQcTask task = new GetSearchQcTask(search.trim(), validatedLimit, validatedOffset, conn);
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