package org.mskcc.limsrest.service.requesttracker;

import com.velox.api.datarecord.DataRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private WorkflowSample root;

    public ProjectSample(Long recordId) {
        this.sampleId = recordId;
        this.complete = true;       // The sample is considered complete until a record is added that is not done

        this.sampleGraph = new HashMap<>();
        this.stages = new HashMap<>();
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

    public Map<String, SampleStageTracker> getStages() {
        return stages;
    }

    public void setStages(Map<String, SampleStageTracker> stages) {
        this.stages = stages;
    }

    /**
     * Merges stages
     *
     * @param stages
     */
    public void addStage(List<SampleStageTracker> stages) {
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
        /*
        apiMap.put("paths", this.paths.stream().map(path -> path.stream()
                .map(sample -> sample.toApiResponse())).collect(Collectors.toList()));
         */
        apiMap.put("stages", this.stages.values().stream().map(
                stage -> stage.toApiResponse()
        ).collect(Collectors.toList()));
        apiMap.put("root", this.root.toApiResponse());

        return apiMap;
    }
}