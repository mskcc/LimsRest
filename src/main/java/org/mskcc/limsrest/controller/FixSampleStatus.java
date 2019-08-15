package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.SetSampleStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
@Deprecated // seems never used
public class FixSampleStatus {

    private final ConnectionPoolLIMS conn;
    private final SetSampleStatus task;
    private final Log log = LogFactory.getLog(AddPoolToFlowcellLane.class);

    public FixSampleStatus(ConnectionPoolLIMS conn, SetSampleStatus setter) {
        this.conn = conn;
        this.task = setter;
    }

    @GetMapping("/fixSampleStatus")
    public String getContent(@RequestParam(value = "sample") String sample,
                             @RequestParam(value = "status") String status, @RequestParam(value = "igoUser") String igoUser) {
        if (!Whitelists.sampleMatches(sample))
            return "FAILURE: sample is not using a valid format";
        log.info("Starting to fix status by user " + igoUser);
        task.init(sample, status, igoUser);
        Future<Object> result = conn.submitTask(task);
        try {
            return "Status:" + result.get();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return "ERROR IN ADDING POOL TO LANE: " + e.getMessage() + " TRACE: " + sw.toString();
        }
    }
}