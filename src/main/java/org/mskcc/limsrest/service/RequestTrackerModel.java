package org.mskcc.limsrest.service;

import java.util.*;
import java.util.stream.Collectors;

public class RequestTrackerModel {
    private List<Map<String, String>> sampleInfo;
    private String projectId;
    private Set<String> recipes;

    public RequestTrackerModel(List<Map<String, String>> sampleInfo, String projectId){
        this.sampleInfo = sampleInfo;
        this.projectId = projectId;

        populateProjectMetadata();
    }

    private void populateProjectMetadata() {
        List<String> recipes = sampleInfo.stream()
                                         .map(info -> info.get("Recipe"))
                                         .collect(Collectors.toList());
        this.recipes = new HashSet(recipes);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", this.projectId);
        map.put("sampleInfo", this.sampleInfo);
        map.put("recipes", this.recipes);

        return map;
    }
}
