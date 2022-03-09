package org.mskcc.limsrest.controller;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.AddOrCreateQCComment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint triggered when adding comments for a project on QC webapp.
 *
* @author Fahimeh Mirhaj
* */

@RestController
@RequestMapping("/")
public class GetQCComment {
    private static Log log = LogFactory.getLog(GetQCComment.class);
    private final ConnectionLIMS conn;

    public GetQCComment(ConnectionLIMS conn) { this.conn = conn; }

    @GetMapping("/getQCComments")
    public Map<String, Pair<String, Date>> getContent(
        @RequestParam(value = "comment") String comment,
        @RequestParam(value = "projectId") String projectId,
        @RequestParam(value = "commentDate") Date commentDate) {

        Map<String, Pair<String, Date>> resp = new HashMap<>();

        if(StringUtils.isBlank(projectId)) {
            log.info("Invalid project id: " + projectId);
            Date d = new Date(); // returns current time in millisecond
            resp.put("error", new Pair("error", d));
            return resp;
        }
        log.info(String.format("Starting to Add comment for project: %s in LIMS", projectId));
        AddOrCreateQCComment task = new AddOrCreateQCComment(projectId, comment, commentDate, conn);


        return task.execute();
    }
}
