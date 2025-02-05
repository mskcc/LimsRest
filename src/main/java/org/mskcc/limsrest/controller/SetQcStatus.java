package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.ToggleSampleQcStatusTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/")
public class SetQcStatus {
    private final static Log log = LogFactory.getLog(SetQcStatus.class);
    private final ConnectionLIMS conn;

    @Value("${airflow_pass}")
    private String airflow_pass;

    public SetQcStatus(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping("/setQcStatus")  // POST called by app.py
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
        if (!qcType.equals("Seq") && !qcType.equals("Post") && !qcType.equals("Ont")) {
            return "ERROR: The only valid qc types for this service are Seq and Post";
        }
        if (qcType.equals("Seq") && recordId == null) {
            return "ERROR: You must specify a valid record id";
        } else if (qcType.equals("Post") && (request == null || sample == null || run == null)) {
            return "ERROR: Post-Sequencing Qc is identified with a triplet: request, sample, run";
        }

        if (recordId != null) {
            long record = Long.parseLong(recordId);
            ToggleSampleQcStatusTask task = new ToggleSampleQcStatusTask(record, status, request, sample, run, qcType, analyst, note, fastqPath, conn, airflow_pass);
            try {
                return "NewStatus:" + task.execute();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return "ERROR IN SETTING REQUEST STATUS: " + e.getMessage();
            }
        } else {
            return "Invalid Record ID";
        }
    }
}