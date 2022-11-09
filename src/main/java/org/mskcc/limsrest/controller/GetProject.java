package org.mskcc.limsrest.controller;

import java.util.List;
import java.util.LinkedList;

import org.mskcc.limsrest.ConnectionLIMS;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.mskcc.limsrest.service.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


@RestController
@RequestMapping("/")
public class GetProject {
    private static Log log = LogFactory.getLog(GetProject.class);
    private final ConnectionLIMS conn;
   
    public GetProject(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping("/getPmProject")  // POST method called by REX
    public List<RequestSummary> getContent(@RequestParam(value = "project") String[] project,
                                           @RequestParam(value = "filter", defaultValue = "false") String filter) {
        log.info("Starting /getPmProject");

        List<RequestSummary> rss = new LinkedList<>();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < project.length; i++) {
            if (!Whitelists.requestMatches(project[i])) {
                log.info("FAILURE: project is not using a valid format");
                return rss;
            } else {
                sb.append(project[i]);
                if (i < project.length - 1) {
                    sb.append(",");
                }
            }
        }
        log.info("Starting get PM project for projects: " + sb.toString());

        GetSamplesTask task = new GetSamplesTask(project, filter, conn);
        return task.execute();
    }
}