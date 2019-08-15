package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.GetIGOCompleteQCTask;
import org.mskcc.limsrest.service.SampleQcSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetIGOCompleteQC {
    private static Log log = LogFactory.getLog(GetIGOCompleteQC.class);

    private final ConnectionPoolLIMS conn;
    private final GetIGOCompleteQCTask task = new GetIGOCompleteQCTask();

    public GetIGOCompleteQC(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getIGOCompleteQC")
    public List<SampleQcSummary> getContent(@RequestParam(value = "sampleId") String sampleId) {
        log.info("Starting /getIGOCompleteQC for sample:" + sampleId);

        if (!Whitelists.sampleMatches(sampleId)) {
            log.error("FAILURE: sampleId is not using a valid format.");
            return null;
        }

        task.init(sampleId);
        Future<Object> result = conn.submitTask(task);
        try {
            return (List<SampleQcSummary>) result.get();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}