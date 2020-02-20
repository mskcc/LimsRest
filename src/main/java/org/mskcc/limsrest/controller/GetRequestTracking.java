package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetRequestTrackingTask;
import org.mskcc.limsrest.service.RequestTrackerModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class GetRequestTracking {
    private final static Log log = LogFactory.getLog(GetRequestTracking.class);

    private final ConnectionLIMS conn;

    public GetRequestTracking(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getRequestTracking")
    public Map<String, Object> getContent(@RequestParam(value = "request") String requestId, HttpServletRequest request) {
        log.info("/getRequestTracking for request:" + requestId + " " + request.getRemoteAddr());

        if (!Whitelists.requestMatches(requestId)) {
            log.error("FAILURE: requestId is not using a valid format.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: requestId is not using a valid format.");
        }

        try {
            GetRequestTrackingTask t = new GetRequestTrackingTask(requestId, conn);
            return t.execute().toMap();
        } catch (Exception e) {
            log.error(e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
