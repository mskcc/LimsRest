package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetInvestigatorDecisionBlankTask;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * Returns a list of requests where the field 'investigatordecision' is blank in the table:
 * QcReportDna,QcReportRna,QcReportLibrary
 */
@RestController
@RequestMapping("/")
public class GetInvestigatorDecisionBlank {
    private final static Log log = LogFactory.getLog(GetInvestigatorDecisionBlank.class);
    private final ConnectionLIMS conn;

    public GetInvestigatorDecisionBlank(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getInvestigatorDecisionBlank")
    public HashMap<String,String> getContent(HttpServletRequest request) {
        log.info("/getInvestigatorDecisionBlank for request:" + request.getRemoteAddr());

        try {
            GetInvestigatorDecisionBlankTask task = new GetInvestigatorDecisionBlankTask(conn);
            HashMap<String,String> result = task.execute();
            return result;
        } catch (Exception e) {
            log.error(e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}