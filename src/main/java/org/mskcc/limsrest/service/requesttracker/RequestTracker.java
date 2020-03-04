package org.mskcc.limsrest.service.requesttracker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.service.GetRequestTrackingTask;

import java.util.*;
import java.util.stream.Collectors;

public class RequestTracker {
    private static Log log = LogFactory.getLog(RequestTracker.class);

    List<Map<String, Object>> samples;
    boolean isComplete;
    List<Map<String, Object>> steps = new ArrayList<>();
    long startTime;
    long updateTime;
    Long deliveryDate;
    Long receivedDate;

    private static String START_TIME = "startTime";
    private static String UPDATE_TIME = "updateTime";

    public RequestTracker(List<SampleTracker> sampleTrackers, Long deliveryDate, Long receivedDate) {
        this.deliveryDate = deliveryDate;
        this.receivedDate = receivedDate;

        this.samples = sampleTrackers.stream()
                .map(tracker -> tracker.toApiResponse())
                .collect(Collectors.toList());
        this.isComplete = isProjectComplete(sampleTrackers);
        this.steps = aggregateSampleSteps(sampleTrackers);

        Optional<Long> projectStartTime = this.steps.stream()
                .map(step -> (Long) step.get(START_TIME))
                .reduce((s1, s2) -> s1 < s2 ? s1 : s2);         // Choose earliest time for the start
        if (projectStartTime.isPresent()) {
            this.startTime = projectStartTime.get();
        }

        Optional<Long> updateTime = this.steps.stream()
                .map(step -> (Long) step.get(UPDATE_TIME))
                .reduce((s1, s2) -> s1 > s2 ? s1 : s2);         // Choose the latest time for the start
        if (updateTime.isPresent()) {
            this.updateTime = updateTime.get();
        }
    }

    private boolean isIgoComplete() {
        return this.deliveryDate != null;
    }

    private List<Map<String, Object>> aggregateSampleSteps(List<SampleTracker> sampleTrackers) {
        Map<String, Map<String, Object>> projectStepsMap = new TreeMap<>();
        for (SampleTracker tracker : sampleTrackers) {
            for (Map.Entry<String, Step> entry : tracker.stepMap.entrySet()) {
                Step step = entry.getValue();
                String name = step.step;

                if (projectStepsMap.containsKey(name)) {
                    Map<String, Object> stepTracker = projectStepsMap.get(name);

                    Boolean currentComplete = (Boolean) stepTracker.get("complete");
                    Integer currentCompletedSamples = (Integer) stepTracker.get("completedSamples");
                    Integer currentTotalSamples = (Integer) stepTracker.get("totalSamples");
                    Long currentUpdateTime = (Long) stepTracker.get(UPDATE_TIME);
                    Long currentStartTime = (Long) stepTracker.get(START_TIME);

                    stepTracker.put("complete", currentComplete && step.complete);
                    stepTracker.put("completedSamples", currentCompletedSamples + step.completedSamples);
                    stepTracker.put("totalSamples", currentTotalSamples + step.totalSamples);
                    stepTracker.put(UPDATE_TIME, currentUpdateTime > step.updateTime ? currentUpdateTime : step.updateTime);
                    stepTracker.put(START_TIME, currentStartTime < step.startTime ? currentStartTime : step.startTime);
                } else {
                    Map<String, Object> stepTracker = new HashMap<>();
                    stepTracker.put("name", name);
                    stepTracker.put("complete", step.complete);
                    stepTracker.put("completedSamples", step.completedSamples);
                    stepTracker.put("totalSamples", step.totalSamples);
                    stepTracker.put(UPDATE_TIME, step.updateTime);
                    stepTracker.put(START_TIME, step.startTime);

                    projectStepsMap.put(name, stepTracker);
                }
            }
        }
        List<Map<String, Object>> steps = new ArrayList<Map<String, Object>>(projectStepsMap.values());

        // Sort steps by their startTime
        Collections.sort(steps, (s1, s2) -> {
            if (!(s1.containsKey(START_TIME) && s2.containsKey(START_TIME)) || s1 == null || s2 == null) {
                log.error(String.format("Could not sort on %s", START_TIME));
                return 0;
            }
            Long s1Start, s2Start;
            try {
                s1Start = (Long) s1.get(START_TIME);
                s2Start = (Long) s2.get(START_TIME);
            } catch (ClassCastException e) {
                log.error(String.format("Failed to parse %s from %s", START_TIME, s1.toString()));
                return 0;
            }

            return s1Start < s2Start ? -1 : s1Start > s2Start ? 1 : 0;
        });

        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).put("order", i);
        }

        return steps;
    }


    private boolean isProjectComplete(List<SampleTracker> sampleTrackers) {
        List<SampleTracker> incompleteSamples = sampleTrackers.stream()
                .filter(tracker -> !tracker.complete)
                .collect(Collectors.toList());
        return incompleteSamples.size() == 0;
    }

    public Map<String, Object> toApiResponse() {
        Map<String, Object> apiResponse = new HashMap<>();

        apiResponse.put("samples", this.samples);
        apiResponse.put("isComplete", this.isComplete);
        apiResponse.put("steps", this.steps);
        apiResponse.put("startTime", this.startTime);
        apiResponse.put("updateTime", this.updateTime);
        apiResponse.put("igoComplete", isIgoComplete());
        apiResponse.put("deliveryDate", this.deliveryDate);
        apiResponse.put("receivedDate", this.receivedDate);

        return apiResponse;
    }

}

