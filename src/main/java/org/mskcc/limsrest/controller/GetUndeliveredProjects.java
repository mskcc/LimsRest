package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetUndeliveredProjectsTask;
import org.mskcc.limsrest.service.RequestSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.mskcc.limsrest.util.Utils.getResponseEntity;

@RestController
@RequestMapping("/")
public class GetUndeliveredProjects {
    private final static Log log = LogFactory.getLog(GetUndeliveredProjects.class);
    private final ConnectionLIMS conn;

    public GetUndeliveredProjects(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getUndeliveredProjects")
    public ResponseEntity<List<RequestSummary>> getContent(@RequestParam(value = "time", required = false) String time) {

        log.info(String.format("/getUndeliveredProjects for projects in past %s days", time));

        // Standardize input
        Integer numDays = 7;
        try {
            numDays = Integer.parseInt(time);
        } catch (NumberFormatException e) {
            log.warn(String.format("Failed to parse time: %s. Using default 7 days", time));
        }
        GetUndeliveredProjectsTask t = new GetUndeliveredProjectsTask(numDays);
        List<RequestSummary> requestTracker = t.execute(this.conn.getConnection());
        return getResponseEntity(requestTracker, HttpStatus.OK);
    }
}
