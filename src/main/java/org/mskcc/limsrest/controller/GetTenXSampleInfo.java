package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetTenXSampleInfoTask;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class GetTenXSampleInfo {
    private Log log = LogFactory.getLog(GetTenXSampleInfo.class);
    private ConnectionLIMS conn;

    public GetTenXSampleInfo(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping("/getTenxSampleInfo")
    public Object getContent(@RequestParam(value="requestId") String requestId) {
        log.info("Starting /getTenxSampleInfo using request ID " + requestId);
        GetTenXSampleInfoTask task = new GetTenXSampleInfoTask(requestId, conn);
        try {
            return task.execute();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}