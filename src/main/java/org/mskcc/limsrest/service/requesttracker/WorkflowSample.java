package org.mskcc.limsrest.service.requesttracker;


import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import com.velox.sloan.cmo.recmodels.SampleModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.platform.commons.util.StringUtils;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.util.LimsStage;

import java.rmi.RemoteException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.mskcc.limsrest.util.StatusTrackerConfig.STAGE_AWAITING_PROCESSING;
import static org.mskcc.limsrest.util.StatusTrackerConfig.getLimsStageFromStatus;
import static org.mskcc.limsrest.util.Utils.*;

/**
 * These are the samples that are created as part of a workflow. There can be many of these samples for each
 * ProjectSample as the original ProjectSample creates children from it in LIMS as it goes through the stages of a
 * workflow.
 *
 * @author David Streid
 */
@Getter @Setter
public class WorkflowSample extends StatusTracker {
    private static Log log = LogFactory.getLog(WorkflowSample.class);

    @Setter(AccessLevel.NONE) Long recordId;
    @Setter(AccessLevel.NONE) @Getter(AccessLevel.NONE) String recordName;
    @Setter(AccessLevel.NONE) @Getter(AccessLevel.NONE) String sourceSampleId;
    String childSampleId;
    @Setter(AccessLevel.NONE) String status;
    WorkflowSample parent;
    @Setter(AccessLevel.NONE) List<WorkflowSample> children;
    DataRecord record;
    Boolean failed;

    // SeqAnalysisSampleQC DataRecords the WorkflowSample is associated with
    @Setter(AccessLevel.NONE) private List<DataRecord> seqAnalysisQcRecords;
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
        this.seqAnalysisQcRecords = new ArrayList<>();

        this.complete = Boolean.FALSE;

        enrichSample(conn);
    }

    public void addSeqAnalysisQcRecords(List<DataRecord> seqAnalysisQcRecords) {
        this.seqAnalysisQcRecords.addAll(seqAnalysisQcRecords);
    }

    public void addChild(WorkflowSample child) {
        this.children.add(child);
    }

    /**
     * Add values for all sample fields that require a database call
     */
    public void enrichSample(ConnectionLIMS conn) {
        if (this.record == null || this.user == null) return;

        String status = getRecordStringValue(this.record, SampleModel.EXEMPLAR_SAMPLE_STATUS, this.user);
        this.sourceSampleId = getRecordStringValue(this.record, SampleModel.SOURCE_LIMS_ID, this.user);
        String stageName = STAGE_AWAITING_PROCESSING;

        DataRecord[] sampleQcRecords = getChildrenofDataRecord(this.record, SeqAnalysisSampleQCModel.DATA_TYPE_NAME, this.user);
        if (sampleQcRecords.length > 0) {
            // Check immediate children (cheaper) prior to checking for all descendants (more expensive)
            try {
                /**
                 *    +-----+     +------+
                 *    | WS1 |     | QC1  |
                 *    +------------------+
                 *       |
                 *    +--v--+
                 *    | WS2 |
                 *    +-----+
                 *       |
                 *    +--v--+     +------+
                 *    | WS3 |     | QC2  |
                 *    +------------------+
                 *
                 * A Sample DataRecord (WS1) can have a SeqAnalysisSampleQCModel child (QC1) AND have children Sample
                 * DataRecords (WS3) that also have SeqAnalysisSampleQCModel children (QC2).
                 * We want all descendant SeqAnalysisSampleQCModel from the input Sample DataRecord, @this.record, b/c:
                 *      - If either QC1 or QC2 is IGO-Complete, the ProjectSample is IGO-Complete
                 *      - If QC1 is failed, it's possible for QC2 to be IGO-Complete
                 * In other words, we can't evaluate on the immediate SeqAnalysisSampleQCModel children of a Workflow
                 * sample
                 */
                List<DataRecord> allDescendingQcSamples = this.record.getDescendantsOfType(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, this.user);
                addSeqAnalysisQcRecords(allDescendingQcSamples);
            } catch (Exception e) {
                log.error(String.format("Unable to retrieve sampleQcRecords from %d", record.getRecordId()));
            }
        }

        try {
            LimsStage limsStage = getLimsStageFromStatus(conn, status);
            stageName = limsStage.getStageName();
        } catch (IllegalArgumentException e) {
            log.error(String.format("Unable to identify stageName for Sample Record %d w/ status '%s'", this.recordId, status));
        }

        super.stage = stageName;
        this.status = status;
        this.failed = isFailedStatus(status);
        super.startTime = getRecordLongValue(this.record, SampleModel.DATE_CREATED, this.user);
        super.updateTime = getRecordLongValue(this.record, SampleModel.DATE_MODIFIED, this.user);
    }

    /**
     * Returns the source request of the workflow sample
     *
     * @return - Source Request ID, e.g. "06302_AG"
     */
    public String getRequestFromSampleId(String sampleId) {
        // Source sample ID has suffix, e.g. "06302_X_1358_1_1". We need to parse out the project, e.g. "06302_X"
        if (!StringUtils.isBlank(sampleId)) {
            String pattern = "\\d{5}_[A-Z,a-z]{1,2}";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(sampleId);
            if (m.find()) {
                return m.group(0);
            }
            return "INVALID";
        }
        return "";
    }

    /**
     * Retrieves a list of the child requestIds for all samples in the subtree of this instance of a WorkflowSample
     *
     * @return - All the child RequestIds (taken from SampleIds) descended from this
     */
    public List<String> getChildRequestIds() {
        List<String> childSampleIds = new ArrayList<>();
        Queue<WorkflowSample> queue = new LinkedList<>();
        queue.add(this);

        // BFS of samples descending from root sample
        WorkflowSample nxt;
        while (queue.size() > 0) {
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
