package org.mskcc.limsrest.service.requesttracker;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.IoError;
import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mskcc.limsrest.util.DataRecordAccess.getRecordLongValue;
import static org.mskcc.limsrest.util.DataRecordAccess.getRecordStringValue;

public class Step {
    private static Log log = LogFactory.getLog(RequestTracker.class);

    public String step;
    public boolean complete;
    public int totalSamples;
    public int completedSamples;
    public long startTime;
    public long updateTime;
    public Set<String> nextSteps;       // Exemplar statuses
    private User user;

    public Step(String step, User user) {
        this.step = step;
        this.complete = true;           // Step is considered complete until a sample is added that isn't done
        this.totalSamples = 0;
        this.completedSamples = 0;
        this.nextSteps = new HashSet<>();
        this.user = user;
    }

    // Records sample passing through step
    public void recordSample(DataRecord record, String recordStatus) {
        Long startTime = getRecordLongValue(record, "DateCreated", this.user);

        // Update time can be null if not updated
        Long updateTime = getRecordLongValue(record, "DateModified", this.user);
        updateTime = updateTime == null ? startTime : updateTime;

        if (startTime != null && (startTime < this.startTime || this.startTime == 0L)) {
            // Update startTime if valid and less than sample's initialized startTime
            this.startTime = startTime;
        }
        if (updateTime != null && updateTime > this.updateTime) {
            this.updateTime = updateTime;
        }

        this.totalSamples += 1;

        DataRecord[] children = new DataRecord[0];
        try {
            children = record.getChildrenOfType("Sample", this.user);
        } catch (IoError | RemoteException e) {
            log.error(String.format("Failed to get children from Record: %d", record.getRecordId()));
        }

        if (children.length > 0) {
            // The sample moved on in the process
            this.completedSamples += 1;
            Set<String> childStatuses = Stream.of(children)
                    .map(sample -> getRecordStringValue(sample, "ExemplarSampleStatus", this.user))
                    .collect(Collectors.toSet());
            this.nextSteps.addAll(childStatuses);
        } else {
            // Sample hasn't moved on and is not complete unless it has completed Illumina Sequencing
            if (!recordStatusIsComplete(recordStatus)) {
                this.complete = false;
            }
        }
    }

    private boolean recordStatusIsComplete(String status) {
        return status.toLowerCase().contains("completed - illumina sequencing");
    }

    public Map<String, Object> jsonify() {
        Map<String, Object> json = new HashMap<>();
        json.put("step", this.step);
        json.put("complete", this.complete);
        json.put("totalSamples", this.totalSamples);
        json.put("completedSamples", this.completedSamples);
        json.put("next", this.nextSteps);
        return json;
    }

}
