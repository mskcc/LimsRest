package org.mskcc.limsrest.service.requesttracker;

import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import static org.mskcc.limsrest.service.requesttracker.StatusTrackerConfig.*;
import static org.mskcc.limsrest.util.Utils.*;

/**
 * Data Model of the tree structure descending from one ProjectSample
 */
public class ProjectSampleTree {
    private static Log log = LogFactory.getLog(ProjectSampleTree.class);

    private WorkflowSample root;                        // Parent Sample of the tree
    private String dataQcStatus;                        // Flag set when the sample's seq-QC results has been passed
    private Map<Long, WorkflowSample> sampleMap;        // Map Record IDs to their enriched sample information
    private Map<String, SampleStageTracker> stageMap;   // Map to all stages by their stage name

    private User user;                                  // TODO - should this be elsewhere?

    public ProjectSampleTree(WorkflowSample root, User user) {
        this.user = user;
        this.root = root;
        this.sampleMap = new HashMap<>();
        this.stageMap = new TreeMap<>(new StatusTrackerConfig.StageComp()); // Order map by order of stages
        this.dataQcStatus = SEQ_QC_STATUS_PENDING;                          // Pending until finding a QcStatus child
    }

    public WorkflowSample getRoot() {
        return root;
    }

    public Boolean isPassedDataQc() {
        return SEQ_QC_STATUS_PASSED.equalsIgnoreCase(this.dataQcStatus);
    }

    public Boolean isFailedDataQC() {
        return SEQ_QC_STATUS_FAILED.equalsIgnoreCase(this.dataQcStatus);
    }

    public void setDataQcStatus(String dataQcStatus) {
        this.dataQcStatus = dataQcStatus;
    }

    public List<WorkflowSample> getSamples() {
        return new ArrayList(sampleMap.values());
    }

    /**
     * Adds sample to the sample map
     *
     * @param sample
     */
    public void addSample(WorkflowSample sample){
        this.sampleMap.put(sample.getRecordId(), sample);
    }

    /**
     * Returns ordered list of stages in the ProjectSample
     * @return
     */
    public List<SampleStageTracker> getStages() {
        return new ArrayList<>(stageMap.values());
    }

    public User getUser() {
        return this.user;
    }

    /**
     * Adds stage to known Stages of this tree
     * @param node
     */
    public void addStageToTracked(WorkflowSample node){
        String stageName = node.getStage();

        if(isValidStage(stageName)){
            SampleStageTracker stage;
            if(this.stageMap.containsKey(stageName)){
                stage = this.stageMap.get(stageName);
                stage.updateStageTimes(node);
            } else {
                stage = new SampleStageTracker(stageName, 1, 0, node.getStartTime(), node.getUpdateTime());
                this.stageMap.put(stageName, stage);
            }

            // TODO - Move
            // Data QC is a special case
            if(STAGE_DATA_QC.equals(stageName) && !isPassedDataQc()){
                // We determine stage completion for the Data QC stage by whether the tree has passed DataQC
                stage.setComplete(Boolean.FALSE);
                if(isFailedDataQC()){
                    // If the input @node had a Failed Data QC child, the Workflow Sample branch needs to be failed
                    node.setFailed(Boolean.TRUE);
                    markFailedBranch(node);
                }

            }
        } else {
            log.warn(String.format("Unable to determine record '%d' stageName '%s'", node.getRecordId(), stageName));
        }
    }

    /**
     * Updates the tree stages and leaf sample completion status
     *      - Tree stage in the stageMap becomes incomplete IF sample's status is not in a completed state
     *      - Leaf completion status needs to be set because there are no children to determine completion
     * @param leaf
     */
    public void updateTreeOnLeafStatus(WorkflowSample leaf){
        SampleStageTracker stage = this.stageMap.computeIfAbsent(leaf.getStage(),
                // TODO - happens whne tree is a single node. Find better fix
                k -> new SampleStageTracker(STAGE_AWAITING_PROCESSING, 1, 0, 0L, 0L));

        // Failed leafs do not modify the completion status
        if(leaf.getFailed()){
            markFailedBranch(leaf);
        } else {
            // If the sample has been recorded as completed sequencing, then the leaf node is completed
            if(isPassedDataQc()){
                leaf.setComplete(Boolean.TRUE);   // Default leaf completion state is FALSE
                stage.setComplete(Boolean.TRUE);  // Reset incompleted stages to true since sequencing is the last step
            } else {
                // Reaching a leaf w/o traversing a node that sets tree to completedSequencing indicates incomplete
                stage.setComplete(Boolean.FALSE);
                if(isFailedDataQC()){
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
        while(parent != null && allChildrenFailed(parent)){
            parent.setFailed(Boolean.TRUE);
            parent = parent.getParent();
        }
        // Record the sample has failed if the parent is null as this means the failed path reached the root
        if(parent == null){
            SampleStageTracker stage = this.stageMap.get(leaf.getStage());
            stage.addFailedSample();
        }
    }

    /**
     * Returns whether input sample has only failed children
     *
     * @param sample
     * @return
     */
    private boolean allChildrenFailed(WorkflowSample sample){
        List<WorkflowSample> children = sample.getChildren();
        for(WorkflowSample child : children){
            if(!child.getFailed()) {
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
        if(this.root == null) return null;

        ProjectSample projectSample = new ProjectSample(this.root.getRecordId());
        List<SampleStageTracker> stages = getStages();
        projectSample.addStages(stages);

        // TODO - Edge case where a sample hasn't started a workflow and has only one "Awaiting Processing" stage

        Boolean isFailed = root.getFailed();                // A failed root indicates 0 branches w/ a non-failed sample
        // ProjectSample completion is determined by all stages
        Boolean isComplete = Boolean.TRUE;
        for(SampleStageTracker stage : stages){
            isComplete = isComplete && stage.getComplete();
        }

        projectSample.setFailed(isFailed);
        projectSample.setComplete(isComplete);
        projectSample.setRoot(getRoot());

        return projectSample;
    }
}
