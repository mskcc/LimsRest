package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.requesttracker.RequestTracker;
import org.mskcc.limsrest.service.requesttracker.SampleTracker;
import org.mskcc.limsrest.util.Pair;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GetRequestTrackingTask {
    private static Log log = LogFactory.getLog(GetRequestTrackingTask.class);

    private ConnectionLIMS conn;
    private String requestId;
    private String serviceId;

    private static String[] FIELDS = new String[] {"ExemplarSampleStatus", "Recipe"};
    private static String[] DATE_FIELDS = new String[] {"DateCreated", "DateModified"};

    public GetRequestTrackingTask(String requestId, String serviceId, ConnectionLIMS conn) {
        this.requestId = requestId;
        this.serviceId = serviceId;
        this.conn = conn;
    }

    private List<DataRecord> getBankedSampleRecords(String serviceId, User user, DataRecordManager drm){
        String query = String.format("ServiceId = '%s'", serviceId);
        List<DataRecord> bankedList = new ArrayList<>();
        try {
             bankedList = drm.queryDataRecords("BankedSample", query, user);
        } catch (NotFound | IoError | RemoteException e){
            log.info(String.format("Could not find BankedSample record for %s", serviceId));
            return null;
        }
        return bankedList;
    }

    // TODO - Maybe accept a String[] fields array that is all the fields that should be pulled from a sample
    public Map<String, Object> execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();

            List<DataRecord> bankedSampleRecords = getBankedSampleRecords(this.serviceId, user, drm);
            if(bankedSampleRecords.isEmpty()){
                throw new IllegalArgumentException(String.format("Invalid serviceID: %s", this.serviceId));
            }

            /*
            if(!mappedRequestId.equals(this.serviceId)){
                throw new IllegalArgumentException("ServiceID does not match requestId");
            }
             */


            List<DataRecord> requestRecordList = drm.queryDataRecords("Request", "RequestId = '" + this.requestId + "'", user);
            if (requestRecordList.size() != 1) {  // error: request ID not found or more than one found
                log.error("Request not found:" + requestId);
                return new HashMap<>();
            }

            // Get all relevant Request information
            DataRecord requestRecord = requestRecordList.get(0);
            Long receivedDate = null;
            Long deliveryDate = null;
            // TODO - exception is thrown if these do not exist
            try {
                receivedDate = requestRecord.getLongVal("ReceivedDate", user);
                deliveryDate = requestRecord.getLongVal("RecentDeliveryDate", user);
            } catch (NullPointerException e){
                // This is an expected exception
            }

            // Immediate samples of the request. These samples represent the overall progress of each project sample
            DataRecord[] samples = requestRecord.getChildrenOfType("Sample", user);
            List<SampleTracker> sampleTrackers = Stream.of(samples)
                    .map(sample -> new SampleTracker(sample, user))
                    .collect(Collectors.toList());

            // Do a BFS using a reference to the sampleTrackers
            List<Pair<DataRecord, SampleTracker>> queue = sampleTrackers.stream()
                    .map(tracker -> (Pair<DataRecord, SampleTracker>) new Pair(tracker.getRecord(), tracker))
                    .collect(Collectors.toList());
            while(queue.size() > 0){
                Pair<DataRecord, SampleTracker> node = queue.remove(0);
                SampleTracker tracker = node.getValue();
                DataRecord parent = node.getKey();

                DataRecord[] children = parent.getChildren(user);
                for(DataRecord record : children){
                    tracker.addSample(record);
                    Pair<DataRecord, SampleTracker> child = new Pair<>(record, tracker);
                    queue.add(child);
                }
            }

            RequestTracker requestTracker = new RequestTracker(sampleTrackers, deliveryDate, receivedDate);
            return requestTracker.toApiResponse();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
