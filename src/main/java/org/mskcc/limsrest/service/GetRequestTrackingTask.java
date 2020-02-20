package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;

import java.util.*;

public class GetRequestTrackingTask {
    private static Log log = LogFactory.getLog(GetRequestTrackingTask.class);

    private ConnectionLIMS conn;
    private String requestId;

    private static String[] FIELDS = new String[] {"ExemplarSampleStatus", "Recipe", "SampleId"};

    public GetRequestTrackingTask(String requestId, ConnectionLIMS conn) {
        this.requestId = requestId;
        this.conn = conn;
    }

    public class SampleTrackingList { }

    // TODO - Maybe accept a String[] fields array that is all the fields that should be pulled from a sample
    public RequestTrackerModel execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();

            List<DataRecord> queue = drm.queryDataRecords("Request", "RequestId = '" + this.requestId + "'", user);
            if (queue.size() != 1) {  // error: request ID not found or more than one found
                log.error("Request not found:" + requestId);
                return new RequestTrackerModel(null, requestId); // SampleTrackingList();
            }

            // Perform BFS from request

            Map<String, Map<String, String>> sampleMap = new HashMap<>();

            String status;
            String sampleId;
            DataRecord[] samples;
            List<DataRecord> childSamples;
            while(queue.size() > 0){
                DataRecord r = queue.remove(0);
                samples = r.getChildrenOfType("Sample", user);
                for(DataRecord s : samples){
                    sampleId = s.getStringVal("SampleId", user);
                    // A pool is child to multiple samples, if it is visited once, no need to go down the rabbit hole again
                    if(!sampleMap.containsKey(sampleId)){
                        Map<String, String> sampleInfo = new HashMap<>();
                        for(String key : FIELDS){
                            sampleInfo.put(key, s.getStringVal(key, user));
                        }
                        sampleMap.put(sampleId, sampleInfo);
                        childSamples = Arrays.asList(s.getChildrenOfType("Sample", user));
                        if(childSamples.size() > 0){
                            queue.addAll(childSamples);
                        }
                    }
                }
            }

            List<Map<String, String>> projectSamples = new ArrayList<> (sampleMap.values());

            return new RequestTrackerModel(projectSamples, this.requestId);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
