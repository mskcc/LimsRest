package org.mskcc.limsrest.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class RequestTrackerModel {
    private List<Map<String, Object>> info;
    private String projectId;
    private String status;                      // Overall status of the project
    private List<Map<String, Object>> steps;    // Steps in the process
    private String trackerType = "LIMS";


    public RequestTrackerModel(List<Map<String, Object>> trackingInfo, String projectId){
        this.info = trackingInfo;
        this.projectId = projectId;

        // populateProjectMetadata(sampleInfo);
    }

    private void populateProjectMetadata(List<Map<String, String>> sampleInfo) {
        this.steps = getSteps(sampleInfo);
        this.status = "Pending";
    }

    /**
     * Calculates the steps in a project based ont he sample's "ExemplarSampleStatus"
     *
     * @return
     */
    private List<Map<String, Object>> getSteps(List<Map<String, String>> sampleInfo) {
        Map<String, Map<String,Object>> stepMap = new HashMap<>();
        String status;
        Map<String,Object> step;
        for(Map<String, String> sample : sampleInfo){
            status = sample.get("ExemplarSampleStatus");
            if(stepMap.containsKey((status))){
                step = stepMap.get(status);
                Integer currentCount = (Integer) step.get("NumSamples");
                step.put("NumSamples", currentCount + 1);
            } else {
                Map<String, Object> newStep = new HashMap<>();
                newStep.put("NumSamples", 1);
                newStep.put("Status", "working");
                newStep.put("status", status);
                stepMap.put(status, newStep);
            }
        }

        return new ArrayList<Map<String, Object>> (stepMap.values());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("trackerType", this.trackerType);
        map.put("date",  LocalDateTime.now());
        map.put("projectId", this.projectId);

        map.put("info", this.info);
        // map.put("steps", this.steps);
        // map.put("status", this);

        return map;
    }
}
