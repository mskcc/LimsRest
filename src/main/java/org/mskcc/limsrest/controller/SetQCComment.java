package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.SetQCCommentTask;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/")
public class SetQCComment {
    private final static Log log = LogFactory.getLog(SetQCComment.class);
    private final ConnectionPoolLIMS conn;

    public SetQCComment(ConnectionPoolLIMS conn) { this.conn = conn; }

    @RequestMapping("/setQcComment")  // POST called by app.py
    public String getContent(@RequestParam(value = "requestId", required = true) String requestId,
                             @RequestParam(value = "comment") String comment,
                             @RequestParam(value = "dateCreated", required = false) Date date,
                             @RequestParam(value = "createdBy", required = false) String createdBy) {
        log.info("Starting to seq Qc comment to:" + comment + " for request " + requestId);

        SetQCCommentTask task = new SetQCCommentTask(requestId, comment, date, createdBy);
        task.execute();

    }
}
