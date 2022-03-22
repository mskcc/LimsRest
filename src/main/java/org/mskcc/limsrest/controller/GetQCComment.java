package org.mskcc.limsrest.controller;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetCreateQCCommentTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

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
    public List<Map<String, Object>> getContent(
        @RequestParam(value = "comment") String comment,
        @RequestParam(value = "projectId") String projectId,
        @RequestParam(value = "commentDate") Date commentDate) {

        List<Map<String, Object>> resp = new LinkedList<>();
        Map<String, Object> eachComment = new HashMap<>();

        if(StringUtils.isBlank(projectId)) {
            log.info("Invalid project id: " + projectId);
            Date d = new Date(); // returns current time in millisecond
            eachComment.put("error", new Pair("error", d));
            resp.add(eachComment);
            return resp;
        }
        log.info(String.format("Starting to Add comment for project: %s in LIMS", projectId));
        GetCreateQCCommentTask task = new GetCreateQCCommentTask(projectId, comment, commentDate, conn);


        return task.execute();
    }
}
