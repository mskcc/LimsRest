package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.cmorequests.CheckAndMarkCMORequestsTask;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class CheckAndMarkCMORequests {
    private static final Log log = LogFactory.getLog(CheckAndMarkCMORequests.class);
    private ConnectionLIMS conn;

    public CheckAndMarkCMORequests(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping(value = "/checkAndMarkCMORequests", method = RequestMethod.POST)
    public String getContent(@RequestParam(value = "projectId", required = false) String projectId,
                             HttpServletRequest request) {

        log.info("Starting /CheckAndMarkCMORequests?projectId=" + projectId + " client IP:" + request.getRemoteAddr());
        CheckAndMarkCMORequestsTask task = new CheckAndMarkCMORequestsTask(conn);
        return task.execute();
    }
}
