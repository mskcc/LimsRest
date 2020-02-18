package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetHiseq;
import org.mskcc.limsrest.service.GetReadyForIllumina;
import org.mskcc.limsrest.service.RunSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class Report {
    private final static Log log = LogFactory.getLog(Report.class);
    private ConnectionLIMS conn;
   
    public Report(ConnectionLIMS conn){
        this.conn = conn;
    }

    @GetMapping("/getHiseq")
    public List<RunSummary> getContent(@RequestParam(value = "run", required = false) String run, @RequestParam(value = "project", required = false) String[] projs) {
        RunSummary rs = new RunSummary("BLANK_RUN", "BLANK_REQUEST");
        List<RunSummary> runSums = new LinkedList<>();
        GetHiseq task;
        if (run != null) {
            if (!Whitelists.textMatches(run)) {
                runSums.add(rs);
                log.info("FAILURE: run is not a valid format");
                return runSums;
            }
            log.info("Starting get Hiseq for run " + run);
            task = new GetHiseq(run, conn);
        } else if (projs != null) {
            for (int i = 0; i < projs.length; i++) {
                if (!Whitelists.requestMatches(projs[i])) {
                    runSums.add(rs);
                    log.info("FAILURE: project is not a valid format");
                    return runSums;
                }
            }
            log.info("Starting get Hiseq for projects");
            task = new GetHiseq(projs, conn);
        } else {
            log.info("Starting get Hiseq with no provided data");
            runSums.add(rs);
            return runSums;
        }
        try {
            runSums = task.execute();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            rs.setInvestigator(e.getMessage() + " TRACE: " + sw.toString());
            runSums.add(rs);
        }
        return runSums;
    }

    @GetMapping("/getHiseqList")
    public List<RunSummary> getContent() {
        log.info("Starting get Hiseq List");
        GetHiseq task = new GetHiseq("", conn);
        List<RunSummary> runSums = new LinkedList<>();
        try {
            runSums = task.execute();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            RunSummary rs = new RunSummary("BLANK_RUN", "BLANK_REQUEST");
            rs.setInvestigator(e.getMessage() + " TRACE: " + sw.toString());
            runSums.add(rs);
        }
        return runSums;
    }

    @GetMapping("/planRuns")
    public List<RunSummary> getPlan(@RequestParam(value = "user") String user) {
        log.info("Starting plan Runs for user " + user);
        GetReadyForIllumina illuminaTask = new GetReadyForIllumina(conn);
        List<RunSummary> runSums = new LinkedList<>();
        try {
            runSums = illuminaTask.execute();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            RunSummary rs = new RunSummary("BLANK_RUN", "BLANK_REQUEST");
            rs.setInvestigator(e.getMessage() + " TRACE: " + sw.toString());
            runSums.add(rs);
        }
        return runSums;
    }
}