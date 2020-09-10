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

import java.util.Map;

@RestController
@RequestMapping("/")
public class UpdateLimsSampleLevelSequencingQc {
    private static Log log = LogFactory.getLog(UpdateLimsSampleLevelSequencingQc.class);
    private final ConnectionLIMS conn;

    public UpdateLimsSampleLevelSequencingQc(ConnectionLIMS conn){
        this.conn = conn;
    }

    @GetMapping("/updateLimsSampleLevelSequencingQc")
    public Map<String, String> getContent(@RequestParam(value = "runId") String runId) {
        if (StringUtils.isBlank(runId)){
            log.info(String.format("Invalid RUN ID: '%s'", runId));
            return null;
        }

        UpdateLimsSampleLevelSequencingQcTask task = new UpdateLimsSampleLevelSequencingQcTask(runId, conn);
        log.info(String.format("Starting to update Sequencing Analysis Sample-Level QC in LIMS for run: %s", runId));
        try {
            return task.execute();
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            return null;
        }
    }
}
