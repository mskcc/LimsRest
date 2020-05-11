package org.mskcc.limsrest.service.requesttracker;


import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mskcc.limsrest.service.requesttracker.StatusTrackerConfig.*;
import static org.mskcc.limsrest.util.DataRecordAccess.getRecordLongValue;
import static org.mskcc.limsrest.util.DataRecordAccess.getRecordStringValue;

/**
 * These are the samples that are created as part of a workflow. There can be many of these samples for each
 * ProjectSample as the original ProjectSample creates children from it in LIMS as it goes through the stages of a
 * workflow.
 */
public class WorkflowSample extends StageTracker {
    private static Log log = LogFactory.getLog(WorkflowSample.class);

    Long recordId;
    String status;
    WorkflowSample parent;     // TODO - Can this ever be multiple?
    List<WorkflowSample> children;
    DataRecord record;
    Boolean failed;
    private User user;
    public WorkflowSample(DataRecord record, User user) {
        setSize(0);

        this.children = new ArrayList<>();
        this.recordId = record.getRecordId();
        this.record = record;
        this.parent = null;
        this.user = user;
        this.failed = Boolean.FALSE;        // TODO - remove this
    }

    public void addChild(WorkflowSample child){
        this.children.add(child);
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

    public WorkflowSample getParent() {
        return parent;
    }

    public void setParent(WorkflowSample parent) {
        this.parent = parent;
    }

    public DataRecord getRecord() {
        return record;
    }

    public void setRecord(DataRecord record) {
        this.record = record;

    }

    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiMap = new HashMap<>();
        apiMap.put("recordId", this.recordId);
        apiMap.put("children", this.children.stream().map(WorkflowSample::toApiResponse));

        Map<String, Object> attributesMap = super.toApiResponse();
        attributesMap.put("status", this.status);
        attributesMap.put("failed", this.failed);
        attributesMap.put("completed", this.complete);
        apiMap.put("attributes", attributesMap);

        return apiMap;
    }
}
