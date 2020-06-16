package org.mskcc.limsrest.service.requesttracker;

import com.velox.api.datarecord.DataRecord;

import java.util.*;
import java.util.stream.Collectors;

import static org.mskcc.limsrest.util.StatusTrackerConfig.*;

/**
 * Representation of each sample in a project. These are the samples IGO's users are aware of,
 *  i.e. User submits a project w/ 12 samples. There are 12 ProjectSamples
 */
public class ProjectSample {
    Long sampleId;
    DataRecord record;
    boolean complete;
    Map<Long, WorkflowSample> sampleGraph;
    Boolean failed;
    private Map<String, SampleStageTracker> stages;

    public WorkflowSample getRoot() {
        return root;
    }

    public void setRoot(WorkflowSample root) {
        this.root = root;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public Boolean getFailed() {
        return failed;
    }

    public void setFailed(Boolean failed) {
        this.failed = failed;
    }

    public List<SampleStageTracker> getStages() {
        return new ArrayList(stages.values());
    }

    /**
     * Adds a list of stages to the map of stages.
     * We merge a Stage when it already exists in the map by updating its update time
     *
     * @param stages
     */
    public void addStages(List<SampleStageTracker> stages) {
        stages.forEach(
                (stage) -> {
                    this.stages.merge(
                            stage.getStage(), stage, (SampleStageTracker currentStage, SampleStageTracker updateStage) -> {
                                currentStage.updateStageTimes(updateStage);
                                return currentStage;
                            });
                }
        );
    }

    public DataRecord getRecord() {
        return this.record;
    }


    /**
     * Needs to be converted into a map to be returned in service response
     *
     * @return
     */
    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiMap = new HashMap<>();

        apiMap.put("sampleId", this.sampleId);
        apiMap.put("status", this.failed ? "Failed" : this.complete == true ? "Complete" : "Pending");
        apiMap.put("stages", this.stages.values().stream().map(
                stage -> stage.toApiResponse()
        ).collect(Collectors.toList()));
        apiMap.put("root", this.root.toApiResponse());

        return apiMap;
    }
}