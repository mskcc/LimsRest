package org.mskcc.limsrest.controller;

import java.util.concurrent.Future;

import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.mskcc.limsrest.service.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
Called by the BIC Validator?
 */
@RestController
@RequestMapping("/")
public class GetPassingSamplesForProject{

    private final ConnectionPoolLIMS conn;
    private final GetPassingSamples task;
    private final Log log = LogFactory.getLog(GetPassingSamplesForProject.class);

    public GetPassingSamplesForProject(ConnectionPoolLIMS conn, GetPassingSamples getPassing ){
        this.conn = conn;
        this.task = getPassing;
    }

    @GetMapping("/getPassingSamplesForProject")
    public RequestSummary getContent(@RequestParam(value = "project") String project, @RequestParam(value = "user") String user,
                                     @RequestParam(value = "year", required = false) String year, @RequestParam(value = "month", required = false) String month, @RequestParam(value = "day", required = false) String day) {
        RequestSummary rs = new RequestSummary();
        Integer dayParam = null;
        Integer monthParam = null;
        Integer yearParam = null;
        if (!((year == null && month == null && day == null) || (year != null && month != null && day != null))) {
            rs.setRestStatus("ERROR: If any of the date fields day, month, year are included, they must all be included");
            return rs;
        } else if (year != null) {
            dayParam = Integer.valueOf(day);
            monthParam = Integer.valueOf(month);
            yearParam = Integer.valueOf(year);
        }

        if (!Whitelists.requestMatches(project)) {
            rs.setRestStatus("ERROR: projectis not using a valid format");
            return rs;
        }
        log.info("Starting to get passing samples for project " + project + " for user " + user);
        task.init(project, dayParam, monthParam, yearParam);
        Future<Object> result = conn.submitTask(task);
        try {
            rs = (RequestSummary) result.get();
        } catch (Exception e) {
            rs.setRestStatus(e.getMessage());
        }
        return rs;
    }
}