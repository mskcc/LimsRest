package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetSampleMetadataTask;
import org.mskcc.cmo.shared.SampleMetadata;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/")
public class GetSampleMetadata {
    private Log log = LogFactory.getLog(org.mskcc.limsrest.controller.GetSampleMetadata.class);
    private ConnectionLIMS conn;
    private String timestamp;
    private String projectId;

    private GetSampleMetadata(ConnectionLIMS conn) {
        this.conn = conn;
    }
    String defaultTimeStamp = String.valueOf(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)));

    @RequestMapping("/getSampleMetadata")
    public List<SampleMetadata> getContent(@RequestParam(value = "timestamp", required = false) String timestamp, @RequestParam(value="projectId", required=false, defaultValue = "NULL") String projectId) {
        this.timestamp = timestamp;
        this.projectId = projectId;
        log.info(String.format("Starting /getSampleMetadata using timestamp: %s and projectId: %s", timestamp,projectId));
        if (timestamp==null){
            timestamp= defaultTimeStamp;
        }
        if (projectId==null || projectId.equalsIgnoreCase("null")){
            projectId=null;
        }
        GetSampleMetadataTask task = new GetSampleMetadataTask(timestamp, projectId, conn);
        try {
            return task.execute();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}

