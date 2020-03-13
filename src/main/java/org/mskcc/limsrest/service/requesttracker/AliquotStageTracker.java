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
        this.failed = Boolean.FALSE;        // TODO - remove this
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
     * Samples w/ ambiguous statuses like "Awaiting Processing" will need to take stage assignment from the most
     * progressed connected sample, i.e. take stage name from child if present, parent if not
     * <p>
     * E.g.
     * Path: "libraryPrep" -> AP_sample  -> "sequencing"   Output: AP_sample.stage = "sequencing"
     * Path: "libraryPrep" -> AP_sample                    Output: AP_sample.stage = "libraryPrep"
     */
    public void assignStageToAmbiguousSamples() {
        if (isValidStage(this.stage)) return;

        if (this.child != null && this.child.getStage() != null) {
            // First try to assign stage from child sample
            this.stage = this.child.getStage();
        } else if (this.parent != null && this.parent.getStage() != null) {
            // If child stage value isn't present, take rom the parent
            this.stage = this.parent.getStage();
        } else {
            log.error(String.format("Could not determine stage for Sample Record: %s. Leaving stage as %s",
                    this.recordId, this.stage));
        }
    }

    /**
     * Add values for all sample fields that require a database call
     */
    public void enrichSample() {
        if (this.record == null || this.user == null) return;

        String status = getRecordStringValue(this.record, "ExemplarSampleStatus", this.user);
        String stage = STAGE_UNKNOWN;
        try {
            stage = getStageForStatus(status);
        } catch (IllegalArgumentException e) {
            log.error(String.format("Unable to identify stage for Sample Record %d w/ status '%s'", this.recordId, status));
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

    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiMap = super.toApiResponse();
        apiMap.put("record", this.recordId);
        apiMap.put("status", this.status);

        return apiMap;
    }
}
