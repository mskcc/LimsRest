package org.mskcc.limsrest.service.requesttracker;


import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

import static org.mskcc.limsrest.service.requesttracker.StatusTrackerConfig.*;
import static org.mskcc.limsrest.util.DataRecordAccess.getRecordLongValue;
import static org.mskcc.limsrest.util.DataRecordAccess.getRecordStringValue;

public class AliquotStageTracker extends StageTracker {
    private static Log log = LogFactory.getLog(AliquotStageTracker.class);

    Long recordId;
    String status;
    AliquotStageTracker parent;     // TODO - Can this ever be multiple?
    AliquotStageTracker child;
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

    public AliquotStageTracker getChild() {
        return child;
    }

    public void setChild(AliquotStageTracker child) {
        this.child = child;
    }

    public Boolean getFailed() {
        return failed;
    }

    public void setFailed(Boolean failed) {
        this.failed = failed;
    }

    /**
     * Add values for all sample fields that require a database call
     */
    public void enrichSample() {
        if (this.record == null || this.user == null) return;

        String status = getRecordStringValue(this.record, "ExemplarSampleStatus", this.user);
        String stage = getStageForStatus(status);

        if (isFailedStatus(status)) {
            this.failed = Boolean.TRUE;
        }
        if (stage.equals(STAGE_UNKNOWN) && this.child != null) {
            // The stage is set to the stage of the child sample
            stage = this.child.getStage();
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

    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiMap = super.toApiResponse();
        apiMap.put("record", this.recordId);
        apiMap.put("status", this.status);

        return apiMap;
    }
}
