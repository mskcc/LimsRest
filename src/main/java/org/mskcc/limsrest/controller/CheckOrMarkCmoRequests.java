package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.cmorequests.CheckOrMarkCmoRequestsTask;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class CheckOrMarkCmoRequests {
    private static final Log log = LogFactory.getLog(CheckOrMarkCmoRequestsTask.class);
    private ConnectionLIMS conn;

    public CheckOrMarkCmoRequests(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping(value = "/checkOrMarkCmoRequests")
    public String getContent(@RequestParam(value = "projectId", required = false, defaultValue = "NULL") String projectId,
                             HttpServletRequest request) {

        log.info("Starting /CheckOrMarkCmoRequests?projectId=" + projectId + " client IP:" + request.getRemoteAddr());
        CheckOrMarkCmoRequestsTask task = new CheckOrMarkCmoRequestsTask(projectId, conn);
        return task.execute();
    }
}
