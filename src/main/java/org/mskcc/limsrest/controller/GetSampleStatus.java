package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetSampleStatusTask;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
public class GetSampleStatus {
    private Log log = LogFactory.getLog(GetSampleStatus.class);
    private ConnectionLIMS conn;

    public GetSampleStatus(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping("/getSampleStatus")
    public String getContent(@RequestParam(value="igoId") String igoId) {
        log.info("Starting /getSampleStatus using IGO ID " + igoId);
        GetSampleStatusTask task = new GetSampleStatusTask(igoId, conn);
        try {
            return task.execute();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}