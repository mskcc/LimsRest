package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.controller.GetSampleStatus;

import java.util.*;

import static org.mskcc.limsrest.util.Utils.*;

/**
 * Task to get current sample status for samples.
 *
 * @author sharmaa1
 */
public class GetSampleStatusTask {

    DataRecordManager dataRecordManager;
    private Log log = LogFactory.getLog(GetSampleStatus.class);
    private String igoId;
    private ConnectionLIMS conn;

    public GetSampleStatusTask(String igoId, ConnectionLIMS conn) {
        this.igoId = igoId;
        this.conn = conn;
    }


    public String execute() {
        long start = System.currentTimeMillis();
        String status = null;
        String requestId = null;
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            dataRecordManager = vConn.getDataRecordManager();
            log.info("Starting GetSampleStatus task using IGO ID " + igoId);
            List<DataRecord> samples = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + igoId + "'", user);
            log.info("Num Sample Records: " + samples.size());
            if (samples.size()==1){
                DataRecord sample = samples.get(0);
                requestId = (String)getValueFromDataRecord(sample, "RequestId", "String", user);
                status = getMostAdvancedLimsStage(samples.get(0), requestId, this.conn);
            }
            log.info("request id: " + requestId);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
        log.info("Total time: " + (System.currentTimeMillis()-start) + " ms");
        log.info("status: " + status);
        return status;
    }
}
