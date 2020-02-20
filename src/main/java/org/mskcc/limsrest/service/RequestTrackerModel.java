package org.mskcc.limsrest.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class RequestTrackerModel {
    private List<Map<String, String>> info;
    private String projectId;
    private Set<String> recipes;
    private String status;                      // Overall status of the project
    private List<Map<String, String>> steps;    // Steps in the process

    private String trackerType = "LIMS";


    public RequestTrackerModel(List<Map<String, String>> sampleInfo, String projectId){
        this.info = sampleInfo;
        this.projectId = projectId;

        populateProjectMetadata();
    }

    private void populateProjectMetadata() {
        List<String> recipes = this.info.stream()
                                         .map(entry -> entry.get("Recipe"))
                                         .collect(Collectors.toList());
        this.recipes = new HashSet(recipes);

        // Calculate Overall status

        // Calculate Overall step

    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("trackerType", this.trackerType);
        map.put("date",  LocalDateTime.now());
        map.put("projectId", this.projectId);

        map.put("sampleInfo", this.info);
        map.put("recipes", this.recipes);
        return map;
    }
}
