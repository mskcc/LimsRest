package org.mskcc.limsrest.controller;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetRequestSamplesTask;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/")
public class GetRequestSamples {
    private final static Log log = LogFactory.getLog(GetRequestSamples.class);

    private final ConnectionLIMS conn;

    public GetRequestSamples(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/api/getRequestSamples")
    public GetRequestSamplesTask.RequestSampleList getContent(@RequestParam(value = "request") String requestId, HttpServletRequest request) {
        log.info("/api/getRequestSamples for request:" + requestId + " " + request.getRemoteAddr());

        if (!Whitelists.requestMatches(requestId)) {
            log.error("FAILURE: requestId is not using a valid format.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: requestId is not using a valid format.");
        }

        try {
            GetRequestSamplesTask t = new GetRequestSamplesTask(requestId, conn);
            GetRequestSamplesTask.RequestSampleList sl = t.execute();
            if ("NOT_FOUND".equals(sl.getRequestId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, requestId + " Request Not Found");
            }
            return sl;
        } catch (Exception e) {
            log.error(e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
