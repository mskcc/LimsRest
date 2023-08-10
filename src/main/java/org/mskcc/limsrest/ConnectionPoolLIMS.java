package org.mskcc.limsrest;

import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.mskcc.limsrest.service.LimsTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * LIMS connection pool with dedicated LIMS connections.
 */
public class ConnectionPoolLIMS {
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    private final VeloxConnection conn1;

    public ConnectionPoolLIMS(String host, int port, String guid, String user1, String pass1) {
        conn1 = new VeloxConnection(host, port, guid, user1, pass1);
    }

    public VeloxConnection getConnection() {
        if (!conn1.isConnected()) { // TODO check other connection is open or it must be since only 2 threads?
            try {
                boolean opened = conn1.open();
                System.out.println("Attempt to re-open connection with result: " + opened);
            } catch (Exception e) {
                System.out.println("Failed to re-open connection: " + e.getMessage());
            }
            return conn1;
        }
        return conn1;
    }

    public Future<Object> submitTask(LimsTask task) {
        task.setConnectionPool(this);
        return executor.submit(task);
    }

    public void cleanup() {
        executor.shutdown();
        try {
            if (conn1 != null && conn1.isConnected()) {
                //conn1.close();
            }
        } catch (Exception e) {
        }
    }
}