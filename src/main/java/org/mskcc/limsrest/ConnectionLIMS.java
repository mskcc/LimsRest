package org.mskcc.limsrest;

import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxStandaloneManagerContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConnectionLIMS {
    private static Log log = LogFactory.getLog(ConnectionLIMS.class);

    private final VeloxConnection conn;
    private User user;
    private DataRecordManager dataRecordManager;
    private DataMgmtServer dataMgmtServer;
    private VeloxStandaloneManagerContext managerContext;

    public ConnectionLIMS(String host, int port, String guid, String user, String pass) {
        conn = new VeloxConnection(host, port, guid, user, pass);
    }

    protected synchronized void connect() {
        if (conn.isConnected())
            return;

        try {
            log.info("Opening LIMS connection.");
            conn.open();
            if (conn.isConnected()) {
                user = conn.getUser();
                dataRecordManager = conn.getDataRecordManager();
                dataMgmtServer = conn.getDataMgmtServer();
                managerContext = new VeloxStandaloneManagerContext(user, dataMgmtServer);
            }
        } catch (Exception e) {
            log.error("Connection error:" + e);
        }
    }

    public User getUser() {
        if (!conn.isConnected())
            connect();
        return user;
    }

    public DataRecordManager getDataRecordManager() {
        if (!conn.isConnected())
            connect();
        return dataRecordManager;
    }

    public void close() {
        if (conn.isConnected()) {
            try {
                conn.close();
            } catch (Exception e) {
            }
        }
    }
}
