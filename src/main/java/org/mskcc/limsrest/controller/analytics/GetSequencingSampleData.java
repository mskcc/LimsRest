package org.mskcc.limsrest.controller.analytics;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetSequencingSampleDataTask;
import org.mskcc.limsrest.service.analytics.SequencingSampleData;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
public class GetSequencingSampleData {

    private Log log = LogFactory.getLog(SequencingSampleData.class);
    private ConnectionLIMS conn;

    public GetSequencingSampleData(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping("/getSequencingSampleData")
    public List<SequencingSampleData> getContent(@RequestParam(value="timestamp") String timestamp) {
        log.info("Starting /getSequencingSampleData using timestamp " + timestamp);
        GetSequencingSampleDataTask task = new GetSequencingSampleDataTask(timestamp, conn);
        try {
            return task.execute();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
