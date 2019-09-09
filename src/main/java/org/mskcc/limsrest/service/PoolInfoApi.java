package org.mskcc.limsrest.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.controller.SetQcStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class PoolInfoApi {
    @Autowired
    private SetQcStatus setQcStatus;

    private final static Log log = LogFactory.getLog(SetQcStatus.class);
    private final ConnectionPoolLIMS conn;

    public PoolInfoApi(ConnectionPoolLIMS conn){
        this.conn = conn;
    }

    /**
     * Given a SeqAnalysisSampleQC recordId, set status of its pooled sample. This will most likely involve traversing
     * the LIMS tree hierarchy from the input record to find the right sample
     *
     * @param recordId, SeqAnalysisSampleQC record Id
     * @param status - Status to set pooled sample to
     * @param user
     * @return
     */
    @RequestMapping("/setPooledSampleStatus")
    public String getContent(@RequestParam(value = "record", required = true) String recordId,
                             @RequestParam(value = "status", required = true) String status,
                             String user) {
        log.info("Searching for record: " + recordId);
        if (recordId != null && status != null) {
            String mappedStatus = "Ready for - Pooling of Sample Libraries for Sequencing"; // Sample has differnet repool status
            long record = Long.parseLong(recordId);
            SetPooledSampleStatus task = new SetPooledSampleStatus();
            task.init(record, mappedStatus);
            Future<Object> result = conn.submitTask(task);
            try {
                Boolean success = (Boolean) result.get();
                String response;
                if(success){
                    response = String.format("Set status for record %s to '%s'", recordId, mappedStatus);
                } else {
                    response = String.format("Failed to set status for record %s to '%s'", recordId, mappedStatus);
                }
                log.info(response);
                return response;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return "ERROR in getPoolInformation: " + e.getMessage();
            }
        } else {
            return "Invalid Record ID/Status";
        }
    }
}