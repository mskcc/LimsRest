package org.mskcc.limsrest.service.requesttracker;

import com.velox.api.datarecord.DataRecord;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

import static org.mskcc.limsrest.util.StatusTrackerConfig.StageComp;

/**
 * API Representation of each sample in a project. These are the samples IGO's users are aware of,
 *      i.e. User submits a project w/ 12 samples. There are 12 ProjectSamples
 * Notes:
 *      * Initialized to "complete"
 *      * Should be kept lightweight
 *
 * @author David Streid
 */
@Getter @Setter
public class ProjectSample {
    @Setter(AccessLevel.NONE) Long sampleId;
    @Setter(AccessLevel.NONE) DataRecord record;
    boolean complete;
    Boolean failed;
    @Getter(AccessLevel.NONE) private Map<String, StageTracker> stages;     // Stages present in the project
    @Setter(AccessLevel.NONE) private Map<String, Object> attributeMap;
    private WorkflowSample root;                        // workflowSamples descend from tree root
    private String currentStage;

    public ProjectSample(Long recordId) {
        this.sampleId = recordId;
        this.complete = true;       // The sample is considered complete until a record is added that is not done
        this.stages = new TreeMap<>(new StageComp());
        this.attributeMap = new HashMap<>();
        this.currentStage = "";
    }
    public List<StageTracker> getStages() {
        return new ArrayList(stages.values());
    }

    /**
     * Adds a list of stages to the map of stages.
     * We merge a Stage when it already exists in the map by updating its update time
     *
     * @param stages
     */
    public void addStages(List<StageTracker> stages) {
        stages.forEach(
                (stage) -> {
                    this.stages.merge(
                            stage.getStage(), stage, (StageTracker currentStage, StageTracker updateStage) -> {
                                currentStage.updateStageTimes(updateStage);
                                return currentStage;
                            });
                }
        );
    }

    /**
     * Adds attributes of the physical sample (not a workflow sample)
     *
     * @param attributeMap
     */
    public void addAttributes(Map<String, Object> attributeMap){
        this.attributeMap.putAll(attributeMap);
    }

    /**
     * Needs to be converted into a map to be returned in service response
     *
     * @return
     */
    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiMap = new HashMap<>();
        apiMap.put("sampleId", this.sampleId);
        apiMap.put("status", this.failed ? "Failed" : this.complete == true ? "Complete" : this.currentStage);
        apiMap.put("stages", this.stages.values().stream().map(
                stage -> stage.toApiResponse(true)
        ).collect(Collectors.toList()));
        apiMap.put("root", this.root.toApiResponse());
        apiMap.put("sampleInfo", this.attributeMap);

        return apiMap;
    }
}