package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.RequestSampleList;
import org.mskcc.limsrest.service.GetDataAccessEmailsTask;
import org.mskcc.limsrest.service.GetRequestSamplesTask;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/*
Some MSKCC users may have an email address but not an HPC id so IGO must send a request to HPC to create
the HPC ID for that user so IGO can grant them access to fastq files.
 */
@RestController
@RequestMapping("/")
public class GetDataAccessEmails {
    private final static Log log = LogFactory.getLog(GetDataAccessEmails.class);

    private final ConnectionLIMS conn;

    public GetDataAccessEmails(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getDataAccessEmails")
    public List<String> getContent(HttpServletRequest request) {
        log.info("/getDataAccessEmails for request:" + request.getRemoteAddr());

        try {
            GetDataAccessEmailsTask task = new GetDataAccessEmailsTask(conn);
            List<String> result = task.execute();
            return result;
        } catch (Exception e) {
            log.error(e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
