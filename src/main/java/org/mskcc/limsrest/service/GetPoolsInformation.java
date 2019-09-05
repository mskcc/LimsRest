package org.mskcc.limsrest.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.controller.SetQcStatus;
import org.mskcc.limsrest.controller.Whitelists;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetPoolsInformation {
    private final static Log log = LogFactory.getLog(SetQcStatus.class);
    private final ConnectionPoolLIMS conn;

    public GetPoolsInformation(ConnectionPoolLIMS conn){
        this.conn = conn;
    }

    @RequestMapping("/getPoolInformation")
    public String getContent(@RequestParam(value = "record", required = true) String recordId, String user) {
        log.info("Grabbing pools for record: " + recordId);
        if (recordId != null) {
            long record = Long.parseLong(recordId);
            PoolInformationCollector task = new PoolInformationCollector();
            task.init(record);
            Future<Object> result = conn.submitTask(task);
            try {
                return "NewStatus:" + result.get();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return "ERROR in getPoolInformation: " + e.getMessage();
            }
        } else {
            return "Invalid Record ID";
        }
    }
}