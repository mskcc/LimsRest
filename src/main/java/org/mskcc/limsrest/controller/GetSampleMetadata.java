package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetSampleMetadataTask;
import org.mskcc.limsrest.service.GetWESSampleDataTask;
import org.mskcc.limsrest.service.samplemetadata.SampleMetadata;
import org.mskcc.limsrest.service.sampletracker.WESSampleData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
public class GetSampleMetadata {
    private Log log = LogFactory.getLog(org.mskcc.limsrest.controller.GetSampleMetadata.class);
    private ConnectionLIMS conn;

    private GetSampleMetadata(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping("/getSampleMetadata")
    public List<SampleMetadata> getContent(@RequestParam(value = "timestamp") String timestamp) {
        log.info("Starting /getSampleMetadata using timestamp " + timestamp);
        GetSampleMetadataTask task = new GetSampleMetadataTask(timestamp, conn);
        try {
            return task.execute();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}

