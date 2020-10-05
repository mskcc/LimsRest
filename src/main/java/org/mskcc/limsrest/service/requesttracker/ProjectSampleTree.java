package org.mskcc.limsrest.service.requesttracker;

import java.util.*;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;
import static org.mskcc.limsrest.util.StatusTrackerConfig.*;
import static org.mskcc.limsrest.util.StatusTrackerConfig.isQcStatusIgoComplete;
import static org.mskcc.limsrest.util.Utils.*;

/**
 * Data Model of the tree structure descending from one ProjectSample that is passed into the recursive calls
 * when doing a search of the project tree in the LIMs. This is used to track ProjectSample DURING TRAVERSAL for the
 * following dynamic fields,
 *      - Stages encountered (@sampleMap)
 *      - Data QC Status (@dataQcStatus) -  This status is set by a node in the Data QC Stage and marks whether the
 *                                          corresponding ProjectSample has passed the DataQC stage. This is unlike
 *                                          other stages that are
 *
 * After traversal, the tree w/ the final values of the dynamic fields are translated into a cleaner representation,
 * ProjectSample, via @convertToProjectSample
 *
 * @author David Streid
 */
public class ProjectSampleTree {
    private static Log log = LogFactory.getLog(ProjectSampleTree.class);

    private WorkflowSample root;                        // Parent Sample of the tree
    private String dataQcStatus;                        // Last SeqAnalysisSampleQC status encountered, until a "Passed"
                                                        // status is found, at which point dataQcStatus is never re-set
    private Map<Long, WorkflowSample> sampleMap;        // Map Record IDs to their enriched sample information
    private Map<String, StageTracker> stageMap;   // Map to all stages by their stage name

    private User user;                                  // TODO - should this be elsewhere?

    public ProjectSampleTree(WorkflowSample root, User user) {
        this.user = user;
        this.root = root;
        this.sampleMap = new HashMap<>();
        this.stageMap = new TreeMap<>(new StageComp()); // Order map by order of stages
        this.dataQcStatus = SEQ_QC_STATUS_PENDING;                          // Pending until finding a QcStatus child
    }

    public WorkflowSample getRoot() {
        return root;
    }

    public boolean isQcIgoComplete() {
        return QcStatus.IGO_COMPLETE.toString().equalsIgnoreCase(this.dataQcStatus);
    }

    public boolean isFailedDataQC() {
        return SEQ_QC_STATUS_FAILED.equalsIgnoreCase(this.dataQcStatus);
    }

    public List<WorkflowSample> getSamples() {
        return new ArrayList(sampleMap.values());
    }

    /**
     * Only method of setting the dataQcStatus. Performs check to verify that the status is not already Data QC
     * passed
     *
     * @param status
     */
    private void setDataQcStatus(String status) {
        if (QcStatus.IGO_COMPLETE.toString().equals(this.dataQcStatus)) {
            log.warn(String.format("Not re-seting Tree dataQcStatus to %s. Sample has already been DataQC %s",
                    status, SEQ_QC_STATUS_PASSED));
            return;
        }
        this.dataQcStatus = status;
    }

    /**
     * Adds sample to the sample map
     *
     * @param sample
     */
    public void addSample(WorkflowSample sample) {
        this.sampleMap.put(sample.getRecordId(), sample);
    }

    /**
     * Returns ordered list of stages in the ProjectSample
     *
     * @return
     */
    public List<StageTracker> getStages() {
        return new ArrayList<>(stageMap.values());
    }

    public User getUser() {
        return this.user;
    }

    /**
     * Adds stage to known Stages of this tree
     *
     * @param node
     */
    public void addStageToTracked(WorkflowSample node) {
        String stageName = node.getStage();

        if (!"".equals(stageName) && stageName != null) {
            StageTracker stage;
            if (this.stageMap.containsKey(stageName)) {
                stage = this.stageMap.get(stageName);
                stage.updateStageTimes(node);
            } else {
                stage = new StageTracker(stageName, StageTracker.SAMPLE_COUNT, 0, node.getStartTime(), node.getUpdateTime());
                this.stageMap.put(stageName, stage);
            }
        } else {
            log.warn(String.format("Unable to determine record '%d' stageName '%s'", node.getRecordId(), stageName));
        }
    }

    /**
     * Updates the DataQcStage of the tree if the input node is at the DataQc stage
     *
     * @param node
     */
    public void updateDataQcStage(WorkflowSample node) {
        String nodeStage = node.getStage();
        if (!STAGE_DATA_QC.equals(nodeStage)) {
            // Only records in the DataQc Stage should update the node
            return;
        }

        // Attempt to update the Data Qc Status of the tree if the current status isn't Passed
        boolean isIgoComplete = isQcIgoComplete();
        if (!isIgoComplete) {
            updateDataQcStatus(node.getRecord());
            isIgoComplete = isQcIgoComplete();          // After @updateDataQcStatus, update this flag
        }

        // Update the Tree Data QC Stage
        StageTracker dataQcStage = this.stageMap.get(nodeStage);
        dataQcStage.setComplete(isIgoComplete);

        // If the input @node had a Failed Data QC child, the Workflow Sample branch needs to be failed
        if (isFailedDataQC()) {
            node.setFailed(Boolean.TRUE);
            markFailedBranch(node);
        }
    }

    /**
     * Updates the trees dataQcStatus, which should only happen if the input DataRecord has a SeqAnalysisSample QC child
     * and that child has a valid Qc Status
     *
     * @param record
     * @return
     */
    private void updateDataQcStatus(DataRecord record) {
        DataRecord[] sampleQcRecords = getChildrenofDataRecord(record, SeqAnalysisSampleQCModel.DATA_TYPE_NAME, this.user);
        if (sampleQcRecords.length == 0) {
            // Only a node in the Data QC stage will have a child instance of the SeqAnalysisSampleQC DataType
            return;
        }

        DataRecord sampleQcRecord = sampleQcRecords[0];
        Boolean isIgoComplete = isQcStatusIgoComplete(sampleQcRecord, this.user);
        if(isIgoComplete){
            // Mark "IGO-Complete" because this is not a valid @SEQ_QCSTATUS value. LIMS differentiates a "Passed" &
            // "IGO-Complete" DataQc record by their "DateIgoComplete" value, not their @SEQ_QCSTATUS value
            setDataQcStatus(QcStatus.IGO_COMPLETE.toString());
        } else {
            String seqQcStatus = getRecordStringValue(sampleQcRecord, SeqAnalysisSampleQCModel.SEQ_QCSTATUS, this.user);
            boolean isValidStatus = !"".equals(seqQcStatus);
            if (isValidStatus) {
                setDataQcStatus(seqQcStatus);
            }
        }
    }

    /**
     * Updates the tree stages and leaf sample completion status
     *      - Tree stage in the stageMap becomes incomplete IF sample's status is not in a completed state
     *      - Leaf completion status needs to be set because there are no children to determine completion
     * @param leaf
     */
    public void updateTreeOnLeafStatus(WorkflowSample leaf) {
        StageTracker stage = this.stageMap.computeIfAbsent(leaf.getStage(),
                // Absent when the LIMS tree is a single node in "Awaiting Processing"
                k -> new StageTracker(STAGE_AWAITING_PROCESSING, StageTracker.SAMPLE_COUNT, 0, 0L, 0L));

        // Failed leafs do not modify the completion status
        if (leaf.getFailed()) {
            markFailedBranch(leaf);
        } else {
            // If the sample has been recorded as completed sequencing, then the leaf node is completed
            if (isQcIgoComplete()) {
                leaf.setComplete(Boolean.TRUE);   // Default leaf completion state is FALSE
                stage.setComplete(Boolean.TRUE);  // Reset incompleted stages to true since sequencing is the last step
            } else {
                // Reaching a leaf w/o traversing a node that sets tree to completedSequencing indicates incomplete
                stage.setComplete(Boolean.FALSE);
                if (isFailedDataQC()) {
                    /**
                     * If a DFS hasn't found a "passed" "SeqAnalysisSampleQC" child, but did find a failed one in the
                     * tree, then the leaf is failed
                     * Note, if a successful DataQC path is traversed first, then all failed DataQC leaves will not fail
                     */
                    markFailedBranch(leaf);
                }
            }
        }
    }

    /**
     * Retrace branch from the leaf and mark all nodes in path as failed if there are no other non-failed children
     *
     * @param leaf
     */
    private void markFailedBranch(WorkflowSample leaf) {
        leaf.setComplete(true);
        // Fail all nodes in path to failed leaf until reaching root (parent == null) or node w/ non-failed children
        WorkflowSample parent = leaf.getParent();
        while (parent != null && allChildrenFailed(parent)) {
            parent.setFailed(Boolean.TRUE);
            parent = parent.getParent();
        }
        // Record the sample has failed if the parent is null as this means the failed path reached the root
        if (parent == null) {
            StageTracker stage = this.stageMap.get(leaf.getStage());
            stage.addFailedSample();
        }
    }

    /**
     * Returns whether input sample has only failed children
     *
     * @param sample
     * @return
     */
    private boolean allChildrenFailed(WorkflowSample sample) {
        List<WorkflowSample> children = sample.getChildren();
        for (WorkflowSample child : children) {
            if (!child.getFailed()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts tree representation into a project Sample
     *
     * @return
     */
    public ProjectSample convertToProjectSample() {
        if (this.root == null) return null;

        ProjectSample projectSample = new ProjectSample(this.root.getRecordId());
        List<StageTracker> stages = getStages();
        projectSample.addStages(stages);

        Boolean isFailed = root.getFailed();                // A failed root indicates 0 branches w/ a non-failed sample
        // ProjectSample completion is determined by all stages
        Boolean isComplete = Boolean.TRUE;
        for (StageTracker stage : stages) {
            isComplete = isComplete && stage.getComplete();
        }

        projectSample.setFailed(isFailed);
        projectSample.setComplete(isComplete);
        projectSample.setRoot(getRoot());

        return projectSample;
    }
}
