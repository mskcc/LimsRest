package org.mskcc.limsrest.controller.sequencingqc;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.sequencingqc.SampleSequencingQcONT;
import org.mskcc.limsrest.service.sequencingqc.SetStatsONTTask;
import org.mskcc.limsrest.service.sequencingqc.UpdateLimsSampleLevelSequencingQcTask;
import org.mskcc.limsrest.service.sequencingqc.UpdateTenXSampleLevelStatsTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Endpoint to Add/Update Sequencing QC Stats to LIMS SeqAnalysisSampleQC DataType. The endpoint is planned to run as
 * soon as new Sequencing Stats are posted to NGS-STATS database.
 * If project id specified put blank in SeqAnalysisSampleQC in LIMS
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
    public Map<String, String> getContent(@RequestParam(value = "runId") String runId, @RequestParam(value = "projectId" , required = false) String projectId) {
        Map<String, String> resp = new HashMap<>();
        if (StringUtils.isBlank(runId)){
            final String error = String.format("Invalid RUN ID: '%s'", runId);
            resp.put("error", error);
            log.info(error);
            return resp;
        }

        UpdateLimsSampleLevelSequencingQcTask task = new UpdateLimsSampleLevelSequencingQcTask(runId, projectId, conn);
        log.info(String.format("Starting to Add/Update SeqAnalysisSampleQC in LIMS for run: %s", runId));
        try {
            return task.execute();
        } catch (Exception e) {
            String err = String.format("%s -> Error while updating Run Stats: %s", ExceptionUtils.getRootCauseMessage(e), ExceptionUtils.getStackTrace(e));
            resp.put("error", err);
            log.info(err);
            return resp;
        }
    }
    @GetMapping("/updateLimsSampleLevelSequencingQcONT")
    public String saveLIMS(@RequestParam(value = "igoId") String igoId,
                           @RequestParam(value = "flowcell") String flowcell,
                           @RequestParam(value = "reads") Long reads,
                           @RequestParam(value = "bases") Double bases,
                           @RequestParam(value = "N50") Long N50,
                           @RequestParam(value = "medianReadLength") Double medianReadLength,
                           @RequestParam(value = "estimatedCoverage", defaultValue = "0.0", required = false) Double estimatedCoverage,
                           @RequestParam(value = "bamCoverage", defaultValue = "0.0", required = false) Double bamCoverage,
                           @RequestParam(value = "sequencerName", defaultValue = "", required = false) String sequencerName,
                           @RequestParam(value = "sequencerPosition") String sequencerPosition) {
        log.info(String.format("Starting to Add/Update ONT stats in LIMS for IGO ID: %s", igoId));

        SampleSequencingQcONT statsONT = new SampleSequencingQcONT(igoId, flowcell, reads, bases, N50,
                medianReadLength, estimatedCoverage, bamCoverage, sequencerName, sequencerPosition);
        SetStatsONTTask task = new SetStatsONTTask(statsONT, conn);
        try {
            return task.execute();
        } catch (Exception e) {
            String err = String.format("%s -> Error while updating ONT Stats: %s", ExceptionUtils.getRootCauseMessage(e), ExceptionUtils.getStackTrace(e));
            log.info(err);
            return err;
        }
    }

    @GetMapping("/updateTenXSampleLevelStats")
    public Map<String, String> tenXStatsToSave(@RequestParam(value = "runId") String runId, @RequestParam(value = "projectId" , required = false) String projectId) {
        Map<String, String> resp = new HashMap<>();
        if (StringUtils.isBlank(runId)) {
            final String error = String.format("Invalid RUN ID: '%s'", runId);
            resp.put("error", error);
            log.info(error);
            return resp;
        }
        UpdateTenXSampleLevelStatsTask task = new UpdateTenXSampleLevelStatsTask(runId, projectId, this.conn);
        log.info(String.format("Starting to Add/updateTenXSampleLevelStats in LIMS for run: %s", runId));
        try {
            return task.execute();
        } catch (Exception e) {
            String err = String.format("%s -> Error while updating Run Stats: %s", ExceptionUtils.getRootCauseMessage(e), ExceptionUtils.getStackTrace(e));
            resp.put("error", err);
            log.info(err);
            return resp;
        }
    }
}