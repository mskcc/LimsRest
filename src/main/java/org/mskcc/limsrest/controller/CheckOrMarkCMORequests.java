package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.cmorequests.CheckOrMarkCMORequestsTask;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class CheckOrMarkCMORequests {
    private static final Log log = LogFactory.getLog(CheckOrMarkCMORequestsTask.class);
    private ConnectionLIMS conn;

    public CheckOrMarkCMORequests(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping(value = "/checkOrMarkCMORequests")
    public String getContent(@RequestParam(value = "projectId", required = false, defaultValue = "NULL") String projectId,
                             HttpServletRequest request) {

        log.info("Starting /CheckOrMarkCMORequests?projectId=" + projectId + " client IP:" + request.getRemoteAddr());
        CheckOrMarkCMORequestsTask task = new CheckOrMarkCMORequestsTask(projectId, conn);
        return task.execute();
    }
}
