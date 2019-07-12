package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GetSamplesInRequestTask;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.Future;

@RestController
public class GetSamplesInRequest {
    private Log log = LogFactory.getLog(GetSamplesInRequest.class);

    private final ConnectionQueue connQueue;
    private final GetSamplesInRequestTask task;

    public GetSamplesInRequest(ConnectionQueue connQueue, GetSamplesInRequestTask task) {
        this.connQueue = connQueue;
        this.task = task;
    }

    @RequestMapping("/getSamplesInRequest")
    public GetSamplesInRequestTask.RequestSampleList getContent(@RequestParam(value = "request") String requestId, @RequestParam(value="tumorOnly", defaultValue="True") Boolean tumorOnly) {
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