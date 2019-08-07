package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionPoolLIMS;
import org.mskcc.limsrest.limsapi.GetRequestSamplesTask;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetRequestSamples {
    private final static Log log = LogFactory.getLog(GetRequestSamples.class);

    private final ConnectionPoolLIMS conn;
    private final GetRequestSamplesTask task = new GetRequestSamplesTask();

    public GetRequestSamples(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/api/getRequestSamples")
    public GetRequestSamplesTask.RequestSampleList getContent(@RequestParam(value = "request") String requestId, @RequestParam(value="tumorOnly", defaultValue="False") Boolean tumorOnly) {
        log.info("Starting /getRequestSamples for requestId:" + requestId);

        if (!Whitelists.requestMatches(requestId)) {
            log.error("FAILURE: requestId is not using a valid format.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: requestId is not using a valid format.");
        }

        task.init(requestId, tumorOnly);
        Future<Object> result = conn.submitTask(task);
        GetRequestSamplesTask.RequestSampleList sl;
        try {
            sl = (GetRequestSamplesTask.RequestSampleList) result.get();
        } catch (Exception e) {
            log.error(e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        if ("NOT_FOUND".equals(sl.requestId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, requestId + " Request Not Found");
        }
        return sl;
    }
}