package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.UpdateFromILabsTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class UpdateFromiLabs {
    private final static Log log = LogFactory.getLog(UpdateFromiLabs.class);

    private final ConnectionLIMS conn;

    @Value("${token_igo}")
    private String tokenIGO;

    @Value("${token_cmo}")
    private String tokenCMO;

    public UpdateFromiLabs(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/UpdateLIMSfromILabs")
    public String getContent(HttpServletRequest request) {
        log.info("/UpdateLIMSfromILabs starting, " + request.getRemoteAddr());

        try {
            UpdateFromILabsTask t = new UpdateFromILabsTask(conn, tokenIGO, tokenCMO);
            String result = (String) t.execute();
            log.info("UpdateLIMSfromILabs result: " + result);
            return result;
        } catch (Exception e) {
            log.error(e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}