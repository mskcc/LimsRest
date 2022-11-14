package org.mskcc.limsrest.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.ExemplarConfig;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;

import java.util.List;


public class GetExemplarConfigTask {
    private static Log log = LogFactory.getLog(GetSampleQcTask.class);
    private ConnectionLIMS conn;

    private String igoId;

    public GetExemplarConfigTask(String igoId, ConnectionLIMS conn) {
        this.igoId = igoId;
        this.conn = conn;
    }
    public ExemplarConfig execute() {
        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager drm = vConn.getDataRecordManager();

        try {
            List<DataRecord> visiumSamples = drm.queryDataRecords("VisiumcDNAPrepProtocol1", "SampleId = '" + igoId + "'", user);
            log.info("Found VisiumcDNAPrepProtocol1 records: " + visiumSamples.size());
            if (visiumSamples.size() == 0) {
                log.error("No visium samples found for given IGO ID.");
                return null;
            }
            DataRecord visiumInfo = visiumSamples.get(0);
            String chipPosition = visiumInfo.getStringVal("ChipPosition", user);
            String chipID = visiumInfo.getStringVal("ChipId", user);
            log.info("Found chip ID: " + chipID + " chip position:" + chipPosition);

            // run 2nd LIMS query to determine if preservation is FFPE
            DataRecord sample = drm.queryDataRecords("Sample", "SampleId = '" + igoId + "'", user).get(0);
            String preservation = sample.getStringVal("Preservation", user);
            ExemplarConfig result = new ExemplarConfig(chipPosition, chipID, preservation);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }
}