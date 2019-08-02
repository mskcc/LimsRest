package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.ToggleSampleQcStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;


@RestController
public class SetQcStatus {
    private final Log log = LogFactory.getLog(SetQcStatus.class);
    private final ConnectionQueue connQueue; 
    private final ToggleSampleQcStatus task;
   
    public SetQcStatus( ConnectionQueue connQueue, ToggleSampleQcStatus toggle){
        this.connQueue = connQueue;
        this.task = toggle;
    }

    @RequestMapping("/setQcStatus")
    public String getContent(@RequestParam(value = "record", required = false) String recordId,
                             @RequestParam(value = "status") String status,
                             @RequestParam(value = "project", required = false) String request,
                             @RequestParam(value = "sample", required = false) String sample,
                             @RequestParam(value = "run", required = false) String run,
                             @RequestParam(value = "qcType", defaultValue = "Seq") String qcType,
                             @RequestParam(value = "analyst", required = false) String analyst,
                             @RequestParam(value = "note", required = false) String note,
                             @RequestParam(value = "fastqPath", required = false) String fastqPath,
                             @RequestParam(value = "user", defaultValue = "") String user) {
        // often called by QC site with just 2 args - recordId & status
        log.info("Starting to seq Qc status to:" + status + " for service" + user);
        if (!Whitelists.requestMatches(request)) {
            return "FAILURE: The project is not using a valid format. " + Whitelists.requestFormatText();
        }
        if (!Whitelists.textMatches(status)) {
            return "FAILURE: The status is not using a valid format. " + Whitelists.textFormatText();
        }
        if (!Whitelists.filePathMatches(fastqPath)) {
            return "FAILURE: The fastq path is not using a valid format. " + Whitelists.filePathFormatText();
        }
        if (!qcType.equals("Seq") && !qcType.equals("Post")) {
            return "ERROR: The only valid qc types for this service are Seq and Post";
        }
        if (qcType.equals("Seq") && recordId == null) {
            return "ERROR: You must specify a valid record id";
        } else if (qcType.equals("Post") && (request == null || sample == null || run == null)) {
            return "ERROR: Post-Sequencing Qc is identified with a triplet: request, sample, run";
        }

        if (recordId != null) {
            long record = Long.parseLong(recordId);
            task.init(record, status, request, sample, run, qcType, analyst, note, fastqPath);
            Future<Object> result = connQueue.submitTask(task);
            try {
                return "NewStatus:" + result.get();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return "ERROR IN SETTING REQUEST STATUS: " + e.getMessage();
            }
        } else {
            return "Invalid Record ID";
        }
    }
}