package org.mskcc.limsrest.service.requesttracker;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mskcc.limsrest.util.DataRecordAccess.getRecordStringValue;

public class SampleTracker {
    private static Log log = LogFactory.getLog(SampleTracker.class);

    Long sampleId;
    DataRecord record;
    boolean complete;
    Map<String, Step> stepMap;
    Map<Long, AliquotStageTracker> sampleGraph;
    Boolean failed;
    private User user;
    private List<List<AliquotStageTracker>> paths;
    private Map<String, SampleStageTracker> stages;
    public SampleTracker(DataRecord record, User user) {
        this.record = record;
        this.sampleId = record.getRecordId();
        this.user = user;
        this.complete = true;       // The sample is considered complete until a record is added that is not done

        this.sampleGraph = new HashMap<>();
        this.stepMap = new HashMap<>();
        this.stages = new HashMap<>();
        // addSample(record);
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
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
                                    currentStage.updateStage(updateStage);
                                    return currentStage;
                                }
                        )

        );
    }

    public void setFailed(Boolean failed) {
        this.failed = failed;
    }

    public void setPaths(List<List<AliquotStageTracker>> paths) {
        this.paths = paths;
    }

    public void addPath(List<AliquotStageTracker> path) {
        this.paths.add(path);
    }

    public DataRecord getRecord() {
        return this.record;
    }

    public void addChildrenToRecord(Long recordId, List<Long> childRecords) {
        this.sampleGraph.get(recordId).addChildren(childRecords);
    }

    /**
     * Calculate the stage the overall sample is at based on the route one path
     *
     * @param path

    public void calculateSampleStage(List<Sample> path) {
    String stageName;
    Long startTime;
    Long updateTime;


    Iterator<Sample> itr = path.iterator();
    Sample sample = itr.next();
    Stage stage;
    while (sample != null) {
    stageName = sample.getStage();
    startTime = sample.getStartTime();
    updateTime = sample.getUpdateTime();

    stages.putIfAbsent(stageName, new Stage(stageName, 0, 0, startTime, updateTime));
    stage = stages.get(stageName);

    // Update the start & end times of the stage
    if (stage.startTime > startTime) {
    stage.setStartTime(startTime);
    }
    if (stage.endTime < updateTime) {
    stage.setUpdateTime(updateTime);
    }
    stage.addStartingSample();

    sample = itr.next();
    if (sample != null && sample.getStage() != stageName) {
    // Next stage is different so the sample has completed this stage
    stage.addEndingSample();
    } else if (sample == null) {
    // No more samples left, so the Stage is incomplete
    // TODO - Add logic to determine if status is in end-state
    stage.setComplete(Boolean.FALSE);
    }
    }
    }
     */

    /**
     * Record a sample
     *
     * @param record
     */
    public void addSample(DataRecord record) {
        String recordStatus = getRecordStringValue(record, "ExemplarSampleStatus", this.user);
        Long recordId = record.getRecordId();

        sampleGraph.putIfAbsent(recordId, new AliquotStageTracker(record, this.user));

        // Determine if sample is still pending
        boolean failed = recordStatus.toLowerCase().contains("failed");


        Step step;
        if (stepMap.containsKey(recordStatus)) {
            step = stepMap.get(recordStatus);
        } else {
            step = new Step(recordStatus, this.user);
            stepMap.put(recordStatus, step);
        }
        step.recordSample(record, recordStatus);

        this.complete = this.complete && step.complete;
    }

    /**
     * Returns all paths that have a size greater than the input index
     *
     * @param paths
     * @param idx
     * @return
     */
    private List<List<Map<String, String>>> getRemainingPaths(List<List<Map<String, String>>> paths, int idx) {
        List<List<Map<String, String>>> remainingPaths = new ArrayList<>();
        for (List<Map<String, String>> path : paths) {
            if (path.size() > idx) {
                remainingPaths.add(path);
            }
        }
        return remainingPaths;
    }

    /**
     * Needs to be converted into a map to be returned in service response
     *
     * @return
     */
    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiMap = new HashMap<>();

        apiMap.put("sampleId", this.sampleId);
        apiMap.put("status", complete == true ? "Complete" : "Pending");

        List<Map<String, Object>> sampleTree = this.sampleGraph.values().stream()
                .map(sample -> sample.toApiResponse())
                .collect(Collectors.toList());

        // apiMap.put("sampleTree", sampleTree);
        // apiMap.put("steps", stepMap);

        apiMap.put("paths", this.paths.stream().map(path -> path.stream()
                .map(sample -> sample.toApiResponse())).collect(Collectors.toList()));

        apiMap.put("stages", this.stages.values().stream().map(
                stage -> stage.toApiResponse()
        ).collect(Collectors.toList()));

        return apiMap;
    }
}