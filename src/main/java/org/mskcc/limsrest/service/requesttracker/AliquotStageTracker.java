package org.mskcc.limsrest.service.requesttracker;


import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import org.omg.CORBA.UNKNOWN;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mskcc.limsrest.service.requesttracker.StatusTrackerConfig.*;
import static org.mskcc.limsrest.util.DataRecordAccess.getRecordLongValue;
import static org.mskcc.limsrest.util.DataRecordAccess.getRecordStringValue;

public class AliquotStageTracker extends StageTracker {
    Long recordId;
    String status;
    Set<Long> children;
    AliquotStageTracker parent;     // TODO - Can this ever be multiple?
    DataRecord record;
    Boolean failed;
    private User user;

    public AliquotStageTracker(DataRecord record, User user) {
        setSize(0);

        this.recordId = record.getRecordId();
        this.record = record;
        this.parent = null;
        this.user = user;
        this.failed = Boolean.FALSE;
    }

    public Boolean getFailed() {
        return failed;
    }

    public void setFailed(Boolean failed) {
        this.failed = failed;
    }

    /**
     * Add values for all sample fields that require a database call
     *
     * @param childPath - Path of wrapper classes around child records that descend from this sample
     */
    public void enrichSample(List<AliquotStageTracker> childPath) {
        if (this.record == null || this.user == null) return;

        String status = getRecordStringValue(this.record, "ExemplarSampleStatus", this.user);
        String stage = getStageForStatus(status);

        // If the stage is not known, the stage will be inherited by the last record in the childPath
        if(stage.equals(STAGE_UNKNOWN) && childPath.size() > 0){
            stage = childPath.get(childPath.size()-1).getStage();
        }

        if (isFailedStatus(status)) {
            this.failed = Boolean.TRUE;
        }

        this.status = status;
        super.startTime = getRecordLongValue(record, "DateCreated", this.user);
        super.updateTime = getRecordLongValue(record, "DateModified", this.user);
        super.stage = stage;
    }

    public Long getRecordId() {
        return recordId;
    }

    public String getStatus() {
        return status;
    }

    public AliquotStageTracker getParent() {
        return parent;
    }

    public void setParent(AliquotStageTracker parent) {
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
        Map<String, Object> apiMap = super.toApiResponse();
        apiMap.put("record", this.recordId);
        apiMap.put("status", this.status);
        // map.put("children", this.children);

        return apiMap;
    }
}
