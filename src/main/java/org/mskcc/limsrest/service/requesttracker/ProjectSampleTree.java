package org.mskcc.limsrest.service.requesttracker;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;

import java.util.*;

import static org.mskcc.limsrest.util.StatusTrackerConfig.*;
import static org.mskcc.limsrest.util.Utils.*;

/**
 * Data Model of the tree structure descending from one ProjectSample that is passed into the recursive calls
 * when doing a search of the project tree in the LIMs. This is used to track ProjectSample DURING TRAVERSAL for the
 * following stages encountered (@sampleMap), which are dynamic values (i.e. can change upon traversl)
 *
 * After traversal, the tree w/ the final values of the dynamic fields are translated into a cleaner representation,
 * ProjectSample, via @evaluateProjectSample, which also evaluates the overall state of the sample
 *
 * @author David Streid
 */
@Getter @Setter @ToString
public class ProjectSampleTree {
    private static Log log = LogFactory.getLog(ProjectSampleTree.class);

    private WorkflowSample root;                        // Parent Sample of the tree
    private String dataQcStatus;                        // SeqAnalysisSampleQC status determinining sequencing status
    private Map<Long, WorkflowSample> sampleMap;        // Map Record IDs to their enriched sample information
    private Map<String, StageTracker> stageMap;         // Map to all stages by their stage name
    @Setter(AccessLevel.NONE) private Map<String, Object> sampleData; // contains map with volume & mass
    @Setter(AccessLevel.NONE) private User user;                                  // TODO - should this be elsewhere?
    private boolean isIgoComplete;
    private String correctedInvestigatorSampleId;
    private String sampleName;
    private String investigatorSampleId;

    public ProjectSampleTree(WorkflowSample root, User user) {
        this.user = user;
        this.root = root;
        this.sampleMap = new HashMap<>();
        this.stageMap = new TreeMap<>(new StageComp()); // Order map by order of stages
        this.dataQcStatus = "";                         // Pending until finding a QcStatus child
        this.sampleData = new HashMap<>();
        this.isIgoComplete = false;                     // Defaults to false, must be set true
        this.sampleName = "";
        this.investigatorSampleId = "";
        this.correctedInvestigatorSampleId = "";
    }

    /**
     * Mapping of the stage to the material they use as input
     */
    private static Map<String, String> stageToMaterialMap = new HashMap<String, String>(){{
        put(STAGE_LIBRARY_PREP, "dna_material");
        put(STAGE_LIBRARY_CAPTURE, "library_material");
    }};

    /**
     * Creates a quantity map of the material used
     * @param remainingVolume
     * @param concentration
     * @param concentrationUnits
     * @return
     */
    private Map<String, Object> createQtyMap(Double remainingVolume, Double mass, Double concentration, String concentrationUnits){
        Map<String, Object> quantityMap = new HashMap<>();
        quantityMap.put("volume", remainingVolume);
        quantityMap.put("mass", mass);
        quantityMap.put("concentration", concentration);
        quantityMap.put("concentrationUnits", concentrationUnits);

        return quantityMap;
    }

    /**
     * Enrichces ProjectSample w/ values for quantity taken directly from LIMS Sample DataRecord
     */
    public void enrichQuantity(DataRecord record, String stage) {
        /**
         * The stages we care about are library prep (where DNA/RNA is input) & library capture (where library is input)
         */
        if (STAGE_LIBRARY_PREP.equals(stage) || STAGE_LIBRARY_CAPTURE.equals(stage)) {
            String stageKey = stageToMaterialMap.get(stage);
            if (!this.sampleData.containsKey(stageKey)){
                // Add concentration volume
                Double remainingVolume = getRecordDoubleValue(record, "Volume", this.user);
                Double concentration = getRecordDoubleValue(record, "Concentration", this.user);
                Double mass = getRecordDoubleValue(record, "TotalMass", this.user);
                String concentrationUnits = getRecordStringValue(record, "ConcentrationUnits", this.user);

                // Since Sample DataRecords can have these fields, but aren't actively used, we only populate on non-null
                if (remainingVolume != null && remainingVolume > 0){
                    this.sampleData.put(stageKey, createQtyMap(remainingVolume, mass, concentration, concentrationUnits));
                }
            }
        }
    }

    public boolean isQcIgoComplete() {
        return QcStatus.IGO_COMPLETE.toString().equalsIgnoreCase(this.dataQcStatus);
    }

    public boolean isFailedDataQC() {
        return QcStatus.FAILED.toString().equalsIgnoreCase(this.dataQcStatus);
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
    public void setDataQcStatus(String status) {
        if (QcStatus.IGO_COMPLETE.toString().equals(this.dataQcStatus)) {
            log.warn(String.format("Not re-seting Tree dataQcStatus to %s. Sample has already been DataQC %s",
                    status, QcStatus.PASSED.toString()));
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
                stage.setComplete(Boolean.TRUE);  // Reset incomplete stages to true since sequencing is the last step
            } else {
                // Reaching a leaf w/o traversing a node that sets tree to completedSequencing indicates incomplete
                // Removing - this should be done after the entire ProjectSample tree has been created
                // stage.setComplete(Boolean.FALSE);
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
    public void markFailedBranch(WorkflowSample leaf) {
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
     * Converts tree representation into a project Sample and evaluates overall state
     *
     * @return ProjectSample, simplified representation of the Sample
     */
    public ProjectSample evaluateProjectSample() {
        if (this.root == null) return null;

        ProjectSample projectSample = new ProjectSample(this.root.getRecordId());
        List<StageTracker> stages = getStages();
        projectSample.addStages(stages);

        String currentStageName = "Completed";
        boolean isFailed = root.getFailed();

        if(this.isIgoComplete){
            for(StageTracker stage : stages){
                stage.setComplete(true);
            }
        } else {
            // Mark all stages, except for the last one, complete
            StageTracker stage;
            for(int i = 0; i < stages.size() - 1; i++){
                stage = stages.get(i);
                stage.setComplete(true);
            }
            // Last stage is the current stage
            StageTracker lastStage = stages.get(stages.size() - 1);
            lastStage.setComplete(false);
            currentStageName = lastStage.getStage();
        }

        projectSample.setFailed(isFailed);
        projectSample.setComplete(this.isIgoComplete);
        projectSample.setRoot(getRoot());
        projectSample.setCurrentStage(currentStageName);
        // Populte sampleData w/ data if it is missing - default it to null values
        for(String key : stageToMaterialMap.values()){
            if(!this.sampleData.containsKey(key)){
                this.sampleData.put(key, createQtyMap(0D, 0D, 0D, ""));
            }
        }
        projectSample.addAttributes(this.sampleData);

        Map<String, Object> sampleNames = new HashMap<>();
        sampleNames.put("sampleName", this.sampleName);
        sampleNames.put("investigatorId", this.investigatorSampleId);
        sampleNames.put("correctedInvestigatorId", this.correctedInvestigatorSampleId);
        projectSample.addAttributes(sampleNames);

        return projectSample;
    }
}
