package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetSampleQcTask;
import org.mskcc.limsrest.service.RequestSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping("/")
public class GetProjectQc {
    private static Log log = LogFactory.getLog(GetProjectQc.class);
    private final ConnectionLIMS conn;

    public GetProjectQc(ConnectionLIMS conn){
        this.conn = conn;
    }

    @GetMapping("/getProjectQc")
    public List<RequestSummary> getProjectSummary(@RequestParam(value="project", required = true) String[] project) {
        log.info("Starting get /getProjectQc for projects: " + Arrays.toString(project));

        if (project == null)
            return new LinkedList<>();

        for (int i = 0; i < project.length; i++) {
            if (!Whitelists.requestMatches(project[i])) {
                log.info("FAILURE: project is not using a valid format");
                return new LinkedList<>();
            }
        }
        long start = System.currentTimeMillis();
        GetSampleQcTask task = new GetSampleQcTask(project, conn);

        List<RequestSummary> rss = new LinkedList<>();
        try {
            rss = task.execute();
            long end = System.currentTimeMillis();
            log.info("Elapsed time to run get project Qc is (ms): " + (end - start));
        } catch (Exception e) {
            RequestSummary rs = new RequestSummary();
            rs.setInvestigator(e.getMessage());
            rss.add(rs);
        }
        return rss;
    }
}