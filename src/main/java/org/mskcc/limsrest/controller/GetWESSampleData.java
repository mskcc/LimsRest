package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.GetWESSampleDataTask;
import org.mskcc.limsrest.service.sampletracker.WESSampleData;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetWESSampleData {
    private Log log = LogFactory.getLog(GetWESSampleData.class);
    private final ConnectionPoolLIMS connQueue;
    //private final ConnectionQueue connQueue;
    private final GetWESSampleDataTask task;

    public GetWESSampleData(ConnectionPoolLIMS connQueue, GetWESSampleDataTask task) {
        this.connQueue = connQueue;
        this.task = task;
    }

    @RequestMapping("/getWESSampleData")
    public List<WESSampleData> getContent(@RequestParam(value="timestamp") String timestamp) {
        log.info("Starting /getWESSampleData using timestamp " + timestamp);

        task.init(timestamp);
        Future<Object> result = connQueue.submitTask(task);
        try {
            return (List<WESSampleData>) result.get();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}