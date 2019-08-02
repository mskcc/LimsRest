package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GetSamplesInRequestTask;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.Future;

@RestController
public class GetSamplesInRequest {
    private final Log log = LogFactory.getLog(GetSamplesInRequest.class);

    private final ConnectionQueue connQueue;
    private final GetSamplesInRequestTask task = new GetSamplesInRequestTask();

    public GetSamplesInRequest(ConnectionQueue connQueue) {
        this.connQueue = connQueue;
    }

    @GetMapping("/api/getSamplesInRequest")
    public GetSamplesInRequestTask.RequestSampleList getContent(@RequestParam(value = "request") String requestId, @RequestParam(value="tumorOnly", defaultValue="False") Boolean tumorOnly) {
        log.info("Starting /getSamplesInRequest for requestId:" + requestId);

        if (!Whitelists.requestMatches(requestId)) {
            log.error("FAILURE: requestId is not using a valid format.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: requestId is not using a valid format.");
        }

        task.init(requestId, tumorOnly);
        Future<Object> result = connQueue.submitTask(task);
        GetSamplesInRequestTask.RequestSampleList sl;
        try {
            sl = (GetSamplesInRequestTask.RequestSampleList) result.get();
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