package org.mskcc.limsrest.service.requesttracker;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.mskcc.limsrest.service.requesttracker.StatusTrackerConfig.isCompletedStatus;
import static org.mskcc.limsrest.service.requesttracker.StatusTrackerConfig.isValidStage;

public class SampleTreeTracker {
    private static Log log = LogFactory.getLog(SampleTreeTracker.class);

    public AliquotStageTracker getRoot() {
        return root;
    }

    public void setRoot(AliquotStageTracker root) {
        this.root = root;
    }

    private AliquotStageTracker root;                   // Parent Sample of the tree
    private Map<Long, AliquotStageTracker> sampleMap;   // Map Record IDs to their enriched sample information
    private Map<String, SampleStageTracker> stageMap;   // Map to all stages by their stage name
    private User user;

    public SampleTreeTracker(AliquotStageTracker root, User user) {
        this.user = user;
        this.root = root;
        this.sampleMap = new HashMap<>();
        this.stageMap = new TreeMap<>(new StatusTrackerConfig.StageComp());     // Order map by order of stages
    }

    public Map<Long, AliquotStageTracker> getSampleMap() {
        return sampleMap;
    }

    /**
     * Adds sample to the sample map
     *
     * @param sample
     */
    public void addSample(AliquotStageTracker sample){
        this.sampleMap.put(sample.getRecordId(), sample);
    }

    public void setSampleMap(Map<Long, AliquotStageTracker> sampleMap) {
        this.sampleMap = sampleMap;
    }

    public Map<String, SampleStageTracker> getStageMap() {
        return stageMap;
    }

    public void setStageMap(Map<String, SampleStageTracker> stageMap) {
        this.stageMap = stageMap;
    }

    public User getUser() {
        return user;
    }

    public void updateStage(AliquotStageTracker node){
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
        } else {
            log.warn(String.format("Unable to determine record '%d' stageName '%s'", node.getRecordId(), stageName));
        }
    }

    /**
     * Updates the leaf samples 'stage in the stageMap of the tree to incomplete IF sample's status is not in a completed state
     * @param sample
     */
    public void updateLeafStageCompletionStatus(AliquotStageTracker sample){
        String stageName = sample.getStage();
        String status = sample.getStatus();

        SampleStageTracker stage = this.stageMap.get(stageName);
        if(!isCompletedStatus(status)){
            stage.setComplete(Boolean.FALSE);
        } else {
            if(stage.getComplete() != null){
                // Update completion status, but sample is failed if any leaf node is in an incomplete state
                stage.setComplete(Boolean.TRUE && stage.getComplete());
            } else {
                // Initialize stage to true
                stage.setComplete(Boolean.TRUE);
            }
        }
    }
}
