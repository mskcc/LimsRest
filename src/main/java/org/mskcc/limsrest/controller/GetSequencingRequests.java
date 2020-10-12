package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetSequencingRequestsTask;
import org.mskcc.limsrest.service.RequestSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Long.parseLong;
import static org.mskcc.limsrest.util.Utils.getResponseEntity;

@RestController
@RequestMapping("/")
public class GetSequencingRequests {
    private static Log log = LogFactory.getLog(GetSequencingRequests.class);
    private final ConnectionLIMS conn;

    public GetSequencingRequests(ConnectionLIMS conn) {
        this.conn = conn;
    }

    /**
     * Retrieves sequencing requests, which are requests that have a sample w/ a SeqAnalysisSampleQCModel DataRecord in
     * their hierarchy
     *  E.g.
     *      { days: 7, complete: true }         Return completed requests from past week (failed doesn't matter)
     *      { complete: false, failed: true }   Return incomplete requests INCLUDING those with failed samples
     *      { complete: false, failed: false }  Return incomplete requests EXCLUDING those with failed samples
     *
     * @param days          Number of days from which to query for sequencingrequests
     * @param complete      Whether to return completed sequencing requests
     * @param includeFailed Whether to included samples w/ failed SeqAnalysisSampleQCModel records
     * @param request
     * @return
     */
    @GetMapping("/getSequencingRequests")
    public ResponseEntity<Map<String, Object>> getContent(@RequestParam(value = "days", defaultValue = "7") String days,
                                                          @RequestParam(value = "complete", defaultValue = "true") String complete,
                                                          HttpServletRequest request) {
        String.format("Starting /getSequencingRequests?days=%s&igoComplete=%s client IP: %s",
                days, complete, request.getRemoteAddr());

        Map<String, Object> resp = new HashMap<>();

        Long numDays = 0L;
        try {
            numDays = parseLong(days);
        } catch (NumberFormatException e) {
            String clientMessage = String.format("Couldn't process input days: %s", days);
            log.error(String.format("%s. Error: %s", clientMessage, e.getMessage()));
            resp.put("message", clientMessage);
            resp.put("status", "Failed");
            return getResponseEntity(resp, HttpStatus.BAD_REQUEST);
        }

        Boolean igoComplete = Boolean.parseBoolean(complete);
        if (numDays < 0) {
            String clientMessage = String.format("Requested days must be greater than 0. Received Days: %s", days);
            resp.put("message", clientMessage);
            resp.put("status", "Failed");
            return getResponseEntity(resp, HttpStatus.BAD_REQUEST);
        }

        GetSequencingRequestsTask task = new GetSequencingRequestsTask(numDays, igoComplete);
        List<RequestSummary> requests = task.execute(this.conn.getConnection());
        resp.put("status", "Success");
        resp.put("requests", requests);
        return getResponseEntity(resp, HttpStatus.OK);
    }
}