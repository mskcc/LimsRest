package org.mskcc.limsrest.service.requesttracker;


import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mskcc.limsrest.service.requesttracker.StatusTrackerConfig.getStageForStatus;
import static org.mskcc.limsrest.util.DataRecordAccess.getRecordLongValue;
import static org.mskcc.limsrest.util.DataRecordAccess.getRecordStringValue;

public class Sample extends Tracker {
    Long recordId;
    String status;
    Set<Long> children;
    Sample parent;
    DataRecord record;
    String stage;
    private User user;

    public Sample(DataRecord record, User user) {
        this.recordId = record.getRecordId();
        this.record = record;
        this.parent = null;
        this.user = user;
        this.size = 1;
    }

    /**
     * Add values for all sample fields that require a database call
     */
    public void enrichSample() {
        if (this.record == null || this.user == null) return;

        String status = getRecordStringValue(this.record, "ExemplarSampleStatus", this.user);
        String stage = getStageForStatus(status);

        this.startTime = getRecordLongValue(record, "DateCreated", this.user);
        this.updateTime = getRecordLongValue(record, "DateModified", this.user);
        this.status = status;
        setStage(stage);
    }

    public Long getRecordId() {
        return recordId;
    }

    public String getStatus() {
        return status;
    }

    public Sample getParent() {
        return parent;
    }

    public void setParent(Sample parent) {
        this.parent = parent;
    }

    public DataRecord getRecord() {
        return record;
    }

    public void setRecord(DataRecord record) {
        this.record = record;

    }

    public void addChildren(List<Long> childRecords) {
        this.children.addAll(childRecords);
    }

    public Map<String, Object> toApiResponse() {
        Map<String, Object> map = new HashMap<>();
        map.put("record", this.recordId);
        map.put("status", this.status);
        map.put("stage", this.stage);
        map.put("startTime", this.startTime);
        map.put("updateTime", this.updateTime);
        map.put("size", this.size);
        map.put("complete", this.complete);
        // map.put("children", this.children);

        return map;
    }
}
