package org.mskcc.limsrest.controller.sequencingqc;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.sequencingqc.UpdateLimsSampleLevelSequencingQcTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint to Add/Update Sequencing QC Stats to LIMS SeqAnalysisSampleQC DataType. The endpoint is planned to run as
 * soon as new Sequencing Stats are posted to NGS-STATS database.
 */
@RestController
@RequestMapping("/")
public class UpdateLimsSampleLevelSequencingQc {
    private static Log log = LogFactory.getLog(UpdateLimsSampleLevelSequencingQc.class);
    private final ConnectionLIMS conn;

    public UpdateLimsSampleLevelSequencingQc(ConnectionLIMS conn){
        this.conn = conn;
    }

    @GetMapping("/updateLimsSampleLevelSequencingQc")
    public Map<String, String> getContent(@RequestParam(value = "requestId") String runId) {
        if (StringUtils.isBlank(runId)){
            final String error = String.format("Invalid RUN ID: '%s'", runId);
            Map<String, String> resp = new HashMap<>();
            resp.put("error", error);
            log.info(error);
            return resp;
        }

        UpdateLimsSampleLevelSequencingQcTask task = new UpdateLimsSampleLevelSequencingQcTask(runId, conn);
        log.info(String.format("Starting to Add/Update SeqAnalysisSampleQC in LIMS for run: %s", runId));
        try {
            return task.execute();
        } catch (Exception e) {
            Map<String, String> resp = new HashMap<>();
            String err = String.format("%s -> Error while updating Run Stats: %s", ExceptionUtils.getRootCauseMessage(e), ExceptionUtils.getStackTrace(e));
            resp.put("error", err);
            log.info(err);
            return resp;
        }
    }
}
