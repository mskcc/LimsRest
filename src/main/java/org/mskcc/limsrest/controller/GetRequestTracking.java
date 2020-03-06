package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetRequestTrackingTask;
import org.mskcc.limsrest.service.RequestTrackerModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mskcc.limsrest.util.Utils.getResponseEntity;

@RestController
@RequestMapping("/")
public class GetRequestTracking {
    private final static Log log = LogFactory.getLog(GetRequestTracking.class);

    private final ConnectionLIMS conn;

    public GetRequestTracking(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getRequestTracking")
    public ResponseEntity<Map<String, Map<String, Object>>> getContent(@RequestParam(value = "request") String requestId,
                                                          @RequestParam(value = "serviceId") String serviceId,
                                                          HttpServletRequest request) {
        log.info("/getRequestTracking for request:" + requestId + " " + request.getRemoteAddr());

        if (!Whitelists.requestMatches(requestId)) {
            log.error("FAILURE: requestId is not using a valid format.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: requestId is not using a valid format.");
        }

        try {
            GetRequestTrackingTask t = new GetRequestTrackingTask(requestId, serviceId, conn);
            Map<String, Map<String, Object>>  requestTracker = t.execute();
            return getResponseEntity(requestTracker, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
