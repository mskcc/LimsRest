package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.SamplePair;
import org.mskcc.limsrest.service.GetSamplePairsTask;
import org.mskcc.limsrest.util.IGOTools;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class GetSamplePairs {
    private static Log log = LogFactory.getLog(GetSamplePairs.class);
    private final ConnectionLIMS conn;

    public GetSamplePairs(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getSamplePairs")
    public SamplePair getContent(@RequestParam(value="igoSampleId") String igoId, HttpServletRequest request) {
        log.info("/getSamplePairs:" + igoId + " IP:" + request.getRemoteAddr());

        if (!IGOTools.isValidIGOSampleId(igoId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid IGO Sample ID: " + igoId);

        GetSamplePairsTask samplePairs = new GetSamplePairsTask(igoId, conn);
        return samplePairs.execute();
    }
}