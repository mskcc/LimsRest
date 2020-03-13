package org.mskcc.limsrest.service.requesttracker;

import com.velox.api.datarecord.DataRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SampleTracker {
    Long sampleId;
    DataRecord record;
    boolean complete;
    Map<String, Step> stepMap;
    Map<Long, AliquotStageTracker> sampleGraph;
    Boolean failed;
    private List<List<AliquotStageTracker>> paths;
    private Map<String, SampleStageTracker> stages;

    public SampleTracker(DataRecord record) {
        this.record = record;
        this.sampleId = record.getRecordId();
        this.complete = true;       // The sample is considered complete until a record is added that is not done

        this.sampleGraph = new HashMap<>();
        this.stepMap = new HashMap<>();
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
    public void addStage(Map<String, SampleStageTracker> stages) {
        stages.forEach(
                (updateName, v) ->
                        this.stages.merge(
                                updateName, v, (SampleStageTracker currentStage, SampleStageTracker updateStage) -> {
                                    currentStage.updateStageTimes(updateStage);
                                    return currentStage;
                                }
                        )

        );
    }

    public void setPaths(List<List<AliquotStageTracker>> paths) {
        this.paths = paths;
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

        return apiMap;
    }
}