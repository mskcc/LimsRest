package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Query Request table for deliveries made after a given timestamp.
 */
public class GetDeliveriesTask {
    private static Log log = LogFactory.getLog(GetDeliveriesTask.class);

    protected long timestamp;

    private ConnectionLIMS conn;

    public GetDeliveriesTask(long timestamp, ConnectionLIMS conn) {
        this.timestamp = timestamp;
        this.conn = conn;
    }

    public Object execute() {
        try {
            User user = conn.getUser();
            DataRecordManager dataRecordManager = conn.getDataRecordManager();

            List<DataRecord> recentDeliveries = dataRecordManager.queryDataRecords("Request", "RecentDeliveryDate > " + timestamp, user);
            List<Delivery> deliveries = new ArrayList<>();
            for (DataRecord dr : recentDeliveries) {
                String requestId = dr.getStringVal("RequestId", user);
                long deliveryDate = dr.getLongVal("RecentDeliveryDate", user);
                deliveries.add(new Delivery(requestId, deliveryDate));
            }
            Collections.sort(deliveries);
            log.info("Total deliveries found:" + deliveries.size());
            return deliveries;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static class Delivery implements Comparable<Delivery>{
        private String request;
        private long deliveryDate;

        public Delivery() {}

        public Delivery(String request, long deliveryDate) {
            this.request = request;
            this.deliveryDate = deliveryDate;
        }

        public String getRequest() {
            return request;
        }
        public void setRequest(String request) {
            this.request = request;
        }
        public long getDeliveryDate() {
            return deliveryDate;
        }
        public void setDeliveryDate(long deliveryDate) {
            this.deliveryDate = deliveryDate;
        }

        @Override
        public int compareTo(Delivery o) {
            return Long.compare(deliveryDate, o.deliveryDate);
        }
    }
}