package org.mskcc.limsrest.service.requesttracker;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.service.GetRequestTrackingTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mskcc.limsrest.util.DataRecordAccess.getRecordStringValue;

public class SampleTracker {
    private static Log log = LogFactory.getLog(SampleTracker.class);

    Long sampleId;
    DataRecord record;
    boolean complete;
    Map<String, Step> stepMap;
    private User user;

    public SampleTracker(DataRecord record, User user) {
        this.record = record;
        this.sampleId = record.getRecordId();
        this.stepMap = new HashMap<>();
        this.user = user;
        this.complete = true;       // The sample is considered complete until a record is added that is not done
        addSample(record);
    }

    public DataRecord getRecord(){
        return this.record;
    }

    /**
     * Record a sample
     *
     * @param record
     */
    public void addSample(DataRecord record) {
        String recordStatus = getRecordStringValue(record, "ExemplarSampleStatus", this.user);
        if (recordStatus == null) return;

        Step step;
        if (stepMap.containsKey(recordStatus)) {
            step = stepMap.get(recordStatus);
        } else {
            step = new Step(recordStatus, this.user);
            stepMap.put(recordStatus, step);
        }
        step.recordSample(record, recordStatus);

        this.complete = this.complete && step.complete;
    }

    /**
     * Returns all paths that have a size greater than the input index
     *
     * @param paths
     * @param idx
     * @return
     */
    private List<List<Map<String, String>>> getRemainingPaths(List<List<Map<String, String>>> paths, int idx) {
        List<List<Map<String, String>>> remainingPaths = new ArrayList<>();
        for (List<Map<String, String>> path : paths) {
            if (path.size() > idx) {
                remainingPaths.add(path);
            }
        }
        return remainingPaths;
    }

    /**
     * Needs to be converted into a map to be returned in service response
     *
     * @return
     */
    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiMap = new HashMap<>();

        apiMap.put("sampleId", this.sampleId);
        apiMap.put("status", complete == true ? "Complete" : "Pending");
        apiMap.put("steps", stepMap);

        return apiMap;
    }
}