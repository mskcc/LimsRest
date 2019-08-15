package org.mskcc.limsrest;

import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.mskcc.limsrest.service.LimsTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConnectionPoolLIMS {
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    private final VeloxConnection conn1;
    private final VeloxConnection conn2;

    public ConnectionPoolLIMS(String host, int port, String guid, String user1, String pass1, String user2, String pass2) {
        conn1 = new VeloxConnection(host, port, guid, user1, pass1);
        conn2 = new VeloxConnection(host, port, guid, user2, pass2);
    }

    public VeloxConnection getConnection() {
        // use the connection that is not busy (current logic prefers conn1)
        if (conn1.isConnected()) {
            System.out.println("USING CONNECTION 2");
            return conn2;
        } else {
            System.out.println("USING CONNECTION 1");
            return conn1;
        }
    }

    public Future<Object> submitTask(LimsTask task) {
        task.setConnectionPool(this);
        return executor.submit(task);
    }

    public void cleanup() {
        executor.shutdown();
        try {
            if (conn1 != null && conn1.isConnected()) {
                conn1.close();
            }
            if (conn2 != null && conn2.isConnected()) {
                conn2.close();
            }
        } catch (Exception e) {
        }
    }
}