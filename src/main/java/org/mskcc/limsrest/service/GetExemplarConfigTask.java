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

    public GetExemplarConfigTask(ConnectionLIMS conn) {
        this.conn = conn;
    }
    public ExemplarConfig execute() {
        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager dataRecordManager = vConn.getDataRecordManager();
        DataRecord configInfo;
        ExemplarConfig exemConfig = new ExemplarConfig();
        try {
            log.info("Inside the execute!");
            configInfo = dataRecordManager.queryDataRecords("ExemplarConfig", null, user).get(0);
            exemConfig.setVisiumImagePath(configInfo.getStringVal("VisiumImagePath", user));
        }
        catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
        return exemConfig;
    }
}