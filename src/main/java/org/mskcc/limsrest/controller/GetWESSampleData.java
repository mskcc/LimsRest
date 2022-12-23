package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetWESSampleDataTask;
import org.mskcc.limsrest.service.sampletracker.WESSampleData;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
public class GetWESSampleData {
    private Log log = LogFactory.getLog(GetWESSampleData.class);
    private ConnectionLIMS conn;

    public GetWESSampleData(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping("/getWESSampleData")
    public List<WESSampleData> getContent(@RequestParam(value="timestamp") String timestamp, @RequestParam(value = "timestamp2", defaultValue = "NULL") String timestamp2) {
        log.info("Starting /getWESSampleData using timestamp " + timestamp + " and timestamp2 " + timestamp2);
        GetWESSampleDataTask task = new GetWESSampleDataTask(timestamp, timestamp2, conn);
        try {
            return task.execute();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
