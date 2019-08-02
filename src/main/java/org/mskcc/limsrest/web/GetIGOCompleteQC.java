package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GetIGOCompleteQCTask;
import org.mskcc.limsrest.limsapi.SampleQcSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.Future;

@RestController
public class GetIGOCompleteQC {
    private final Log log = LogFactory.getLog(GetIGOCompleteQC.class);

    private final ConnectionQueue connQueue;
    private final GetIGOCompleteQCTask task = new GetIGOCompleteQCTask();

    public GetIGOCompleteQC(ConnectionQueue connQueue) {
        this.connQueue = connQueue;
    }

    @GetMapping("/getIGOCompleteQC")
    public List<SampleQcSummary> getContent(@RequestParam(value = "sampleId") String sampleId) {
        log.info("Starting /getIGOCompleteQC for sample:" + sampleId);

        if (!Whitelists.sampleMatches(sampleId)) {
            log.error("FAILURE: sampleId is not using a valid format.");
            return null;
        }

        task.init(sampleId);
        Future<Object> result = connQueue.submitTask(task);
        try {
            return (List<SampleQcSummary>) result.get();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}