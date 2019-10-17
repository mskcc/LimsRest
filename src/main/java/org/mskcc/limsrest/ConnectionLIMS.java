package org.mskcc.limsrest;

import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConnectionLIMS {
    private static Log log = LogFactory.getLog(ConnectionLIMS.class);

    private VeloxConnection conn1;
    private VeloxConnection conn2;
    private VeloxConnection inUse;

    public ConnectionLIMS(String host, int port, String guid, String user1, String pass1, String user2, String pass2) {
        conn1 = new VeloxConnection(host, port, guid, user1, pass1);
        try {
            log.info("Opening LIMS connection.");
            conn1.open();
            if (conn1.isConnected()) {
                log.info("LIMS connection established.");
            }
        } catch (Exception e) {
            log.error("Connection error:" + e);
            System.exit(-1);
        }

        inUse = conn1;
    }

    public synchronized VeloxConnection getConnection() {
        return inUse;
    }

    public void close() {
        if (conn1.isConnected()) {
            try {
                conn1.close();
            } catch (Exception e) {
            }
        }
    }
}
