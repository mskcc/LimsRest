package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.GetBillingReport;
import org.mskcc.limsrest.service.RunSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.concurrent.Future;


@RestController
@RequestMapping("/") @Deprecated // code started but not completed or used?
public class BillingReport {

    private final ConnectionPoolLIMS conn;
    private final GetBillingReport task;
    private static Log log = LogFactory.getLog(BillingReport.class);

    public BillingReport(ConnectionPoolLIMS conn, GetBillingReport getBillingReport) {
        this.conn = conn;
        this.task = getBillingReport;
    }

    @GetMapping("/getBillingReport")
    public LinkedList<RunSummary> getContent(@RequestParam(value = "project", required = true) String proj) {
        RunSummary rs = new RunSummary("BLANK_RUN", "BLANK_REQUEST");
        LinkedList<RunSummary> runSums = new LinkedList<>();
        log.info("Starting get billing report for project " + proj);
        task.init(proj);
        Future<Object> result = conn.submitTask(task);
        try {
            runSums = (LinkedList<RunSummary>) result.get();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            rs.setInvestigator(e.getMessage() + " TRACE: " + sw.toString());
            runSums.add(rs);
        }
        return runSums;
    }
}