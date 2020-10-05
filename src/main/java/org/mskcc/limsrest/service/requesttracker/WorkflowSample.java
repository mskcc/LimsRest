package org.mskcc.limsrest.service.requesttracker;


import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import com.velox.sloan.cmo.recmodels.SampleModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.platform.commons.util.StringUtils;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.util.LimsStage;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.mskcc.limsrest.util.StatusTrackerConfig.*;
import static org.mskcc.limsrest.util.Utils.*;

/**
 * These are the samples that are created as part of a workflow. There can be many of these samples for each
 * ProjectSample as the original ProjectSample creates children from it in LIMS as it goes through the stages of a
 * workflow.
 *
 * @author David Streid
 */
public class WorkflowSample extends StatusTracker {
    private static Log log = LogFactory.getLog(WorkflowSample.class);

    Long recordId;
    String recordName;
    String sourceSampleId;
    String childSampleId;
    String status;
    WorkflowSample parent;     // TODO - Can this ever be multiple?
    List<WorkflowSample> children;
    DataRecord record;
    Boolean failed;
    private User user;

    public WorkflowSample(DataRecord record, ConnectionLIMS conn) {
        // Workflow samples don't have a size - they are the extension of the root ProjectSample
        setSize(0);

        this.user = conn.getConnection().getUser();
        this.children = new ArrayList<>();
        this.recordId = record.getRecordId();
        this.recordName = getRecordStringValue(record, SampleModel.SAMPLE_ID, this.user);
        this.record = record;
        this.parent = null;

        this.complete = Boolean.FALSE;

        enrichSample(conn);
    }

    public List<WorkflowSample> getChildren() {
        return children;
    }

    public void addChild(WorkflowSample child) {
        this.children.add(child);
    }

    public Boolean getFailed() {
        return failed;
    }

    public void setFailed(Boolean failed) {
        this.failed = failed;
    }

    public String getChildSampleId() {
        return this.childSampleId;
    }

    public void setChildSampleId(String childSampleId) {
        this.childSampleId = childSampleId;
    }

    /**
     * Add values for all sample fields that require a database call
     */
    public void enrichSample(ConnectionLIMS conn) {
        if (this.record == null || this.user == null) return;

        String status = getRecordStringValue(this.record, SampleModel.EXEMPLAR_SAMPLE_STATUS, this.user);
        this.sourceSampleId = getRecordStringValue(this.record, SampleModel.SOURCE_LIMS_ID, this.user);
        String stageName = STAGE_AWAITING_PROCESSING;
        try {
            // Data QC stage is determined by presence of a SeqQCStatus record, it needs to be re-assigned
            DataRecord[] sampleQcRecord = getChildrenofDataRecord(this.record, SeqAnalysisSampleQCModel.DATA_TYPE_NAME, this.user);
            if (sampleQcRecord.length > 0) {
                stageName = STAGE_DATA_QC;
            } else {
                // If no DataQC records are found, assign stage based on the Exemplar Status
                LimsStage limsStage = getLimsStageFromStatus(conn, status);
                stageName = limsStage.getStageName();
            }
        } catch (IllegalArgumentException e) {
            log.error(String.format("Unable to identify stageName for Sample Record %d w/ status '%s'", this.recordId, status));
        }

        if (isFailedStatus(status)) {
            this.failed = Boolean.TRUE;
        } else {
            this.failed = Boolean.FALSE;
        }

        this.status = status;
        super.startTime = getRecordLongValue(this.record, SampleModel.DATE_CREATED, this.user);
        super.updateTime = getRecordLongValue(this.record, SampleModel.DATE_MODIFIED, this.user);
        super.stage = stageName;
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

    /**
     * Returns the source request of the workflow sample
     *
     * @return - Source Request ID, e.g. "06302_AG"
     */
    public String getRequestFromSampleId(String sampleId) {
        // Source sample ID has suffix, e.g. "06302_X_1358_1_1". We need to parse out the project, e.g. "06302_X"
        if(!StringUtils.isBlank(sampleId)){
            String pattern = "\\d{5}_[A-Z,a-z]{1,2}";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(sampleId);
            if(m.find()){
                return m.group(0);
            }
            return "INVALID";
        }
        return "";
    }

    /**
     * Retrieves a list of the child requestIds for all samples in the subtree of this instance of a WorkflowSample
     * @return - All the child RequestIds (taken from SampleIds) descended from this
     */
    public List<String> getChildRequestIds() {
        List<String> childSampleIds = new ArrayList<>();
        Queue<WorkflowSample> queue = new LinkedList<>();
        queue.add(this);

        WorkflowSample nxt;
        while(queue.size() > 0){
            nxt = queue.remove();
            childSampleIds.add(nxt.getChildSampleId());
            queue.addAll(nxt.getChildren());
        }

        return childSampleIds.stream()
                      .map(sampleId -> getRequestFromSampleId(sampleId))
                      .collect(Collectors.toList());
    }

    public String getSourceRequestId() {
        return getRequestFromSampleId(this.sourceSampleId);
    }

    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiMap = new HashMap<>();
        apiMap.put("recordId", this.recordId);
        apiMap.put("recordName", this.recordName);

        // TODO - for non-admins, exclude this field
        apiMap.put("children", this.children.stream().map(WorkflowSample::toApiResponse));

        Map<String, Object> attributesMap = super.toApiResponse();
        attributesMap.put("status", this.status);
        attributesMap.put("failed", this.failed);
        attributesMap.put("sourceSampleId", this.sourceSampleId);
        attributesMap.put("childSampleId", this.childSampleId);
        apiMap.put("attributes", attributesMap);

        return apiMap;
    }
}
