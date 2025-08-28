package org.mskcc.limsrest;

import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConnectionLIMS {
    private static Log log = LogFactory.getLog(ConnectionLIMS.class);

    private VeloxConnection conn1;
    private VeloxConnection inUse;

    public ConnectionLIMS(String host, int port, String guid, String user1, String pass1) {
        conn1 = new VeloxConnection(host, port, guid, user1, pass1);
        try {
            log.info("Opening LIMS connection to host: " + host + ":" + port + " with guid: " + guid);
            boolean status = conn1.open();
            if (conn1.isConnected()) {
                log.info("LIMS connection established with status: " + status);
            }
        } catch (Exception e) {
            log.error("Connection error:" + e);
            throw new RuntimeException("Failed to open LIMS connection." + host + ":"+port + " User:"+ user1);
        }

        inUse = conn1;
    }

    public synchronized VeloxConnection getConnection() {
        if (!inUse.isConnected()) {
            try {
                boolean opened = inUse.open();
                log.info("Attempt to re-open connection with result: " + opened);
            } catch (Exception e) {
                log.error("Failed to re-open connection: " + e.getMessage());
            }
        }

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
