package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GetRequestTrackingTask {
    private static Log log = LogFactory.getLog(GetRequestTrackingTask.class);

    private ConnectionLIMS conn;
    private String requestId;

    private static String[] FIELDS = new String[] {"ExemplarSampleStatus", "Recipe"};
    private static String[] DATE_FIELDS = new String[] {"DateCreated", "DateModified"};

    private static String START_TIME = "startTime";
    private static String UPDATE_TIME = "updateTime";

    public GetRequestTrackingTask(String requestId, ConnectionLIMS conn) {
        this.requestId = requestId;
        this.conn = conn;
    }


    private String getRecordStringValue(DataRecord record, String key, User user){
        try {
            return record.getStringVal(key, user);
        } catch (NotFound | RemoteException e){
            log.error(String.format("Failed to get key %s from Sample Record: %d", key, record.getRecordId()));
        }
        return "";
    }

    private Long getRecordLongValue(DataRecord record, String key, User user){
        try {
            return record.getLongVal(key, user);
        } catch (NotFound | RemoteException | NullPointerException e){
            log.error(String.format("Failed to get key %s from Sample Record: %d", key, record.getRecordId()));
        }
        return null;
    }

    private class RequestTracker {
        List<Map<String, Object>> samples;
        boolean isComplete;
        List<Map<String, Object>> steps = new ArrayList<>();
        long startTime;
        long updateTime;

        public RequestTracker(List<SampleTracker> sampleTrackers) {
            this.samples = sampleTrackers.stream()
                    .map(tracker -> tracker.toApiResponse())
                    .collect(Collectors.toList());
            this.isComplete = isProjectComplete(sampleTrackers);
            this.steps = aggregateSampleSteps(sampleTrackers);

            Optional<Long> projectStartTime = this.steps.stream()
                    .map(step -> (Long) step.get(START_TIME))
                    .reduce((s1, s2) -> s1 < s2 ? s1 : s2);         // Choose earliest time for the start
            if(projectStartTime.isPresent()){
                this.startTime = projectStartTime.get();
            }

            Optional<Long> updateTime = this.steps.stream()
                    .map(step -> (Long) step.get(UPDATE_TIME))
                    .reduce((s1, s2) -> s1 > s2 ? s1 : s2);         // Choose the latest time for the start
            if(updateTime.isPresent()){
                this.updateTime = updateTime.get();
            }
        }

        private List<Map<String, Object>> aggregateSampleSteps(List<SampleTracker> sampleTrackers){
            Map<String, Map<String, Object>> projectStepsMap = new TreeMap<>();
            for(SampleTracker tracker : sampleTrackers){
                for(Map.Entry<String, Step> entry : tracker.stepMap.entrySet()){
                    Step step = entry.getValue();
                    String name = step.step;

                    if(projectStepsMap.containsKey(name)){
                        Map<String, Object>  stepTracker = projectStepsMap.get(name);

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
                        Map<String, Object>  stepTracker = new HashMap<>();
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
                if( !(s1.containsKey(START_TIME) && s2.containsKey(START_TIME)) || s1 == null || s2 == null){
                    log.error(String.format("Could not sort on %s", START_TIME));
                    return 0;
                }
                Long s1Start, s2Start;
                try {
                    s1Start = (Long) s1.get(START_TIME);
                    s2Start = (Long) s2.get(START_TIME);
                } catch (ClassCastException e){
                    log.error(String.format("Failed to parse %s from %s", START_TIME, s1.toString()));
                    return 0;
                }

                Long diff = s1Start - s2Start;

                return diff.intValue();
            });

            return steps;
        }


        private boolean isProjectComplete(List<SampleTracker> sampleTrackers){
            List<SampleTracker> incompleteSamples = sampleTrackers.stream()
                    .filter(tracker -> !tracker.complete)
                    .collect(Collectors.toList());
            return incompleteSamples.size() == 0;
        }

        public Map<String, Object> toApiResponse(){
            Map<String, Object> apiResponse = new HashMap<>();

            apiResponse.put("samples", this.samples);
            apiResponse.put("isComplete", (Boolean) this.isComplete);
            apiResponse.put("steps", this.steps);
            apiResponse.put("startTime", this.startTime);
            apiResponse.put("updateTime", this.updateTime);

            return apiResponse;
        }

    }

    private class SampleTracker {
        Long sampleId;
        DataRecord record;
        boolean complete;
        Map<String, Step> stepMap;
        private User user;

        public SampleTracker(DataRecord record, User user){
            this.record = record;
            this.sampleId = record.getRecordId();
            this.stepMap = new HashMap<>();
            this.user = user;
            this.complete = true;       // The sample is considered complete until a record is added that is not done
            addSample(record);
        }

        /**
         * Record a sample
         *
         * @param record
         */
        public void addSample(DataRecord record){
            String recordStatus = getRecordStringValue(record, "ExemplarSampleStatus", this.user);
            if(recordStatus == null) return;

            Step step;
            if(stepMap.containsKey(recordStatus)){
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
        private List<List<Map<String, String>>> getRemainingPaths(List<List<Map<String, String>>> paths, int idx){
            List<List<Map<String, String>>> remainingPaths = new ArrayList<>();
            for(List<Map<String, String>> path : paths){
                if(path.size() > idx){
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
        public Map<String, Object> toApiResponse(){
            Map<String, Object> apiMap = new HashMap<>();

            apiMap.put("sampleId", this.sampleId);
            apiMap.put("status", complete == true ? "Complete" : "Pending");
            apiMap.put("steps", stepMap);

            return apiMap;
        }
    }

    private class Step {
        public String step;
        public boolean complete;
        public int totalSamples;
        public int completedSamples;
        public long startTime;
        public long updateTime;
        public Set<String> nextSteps;       // Exemplar statuses
        private User user;

        public Step(String step, User user){
            this.step = step;
            this.complete = true;           // Step is considered complete until a sample is added that isn't done
            this.totalSamples = 0;
            this.completedSamples = 0;
            this.nextSteps = new HashSet<>();
            this.user = user;
        }

        // Records sample passing through step
        public void recordSample(DataRecord record, String recordStatus){
            Long startTime = getRecordLongValue(record, "DateCreated", this.user);

            // Update time can be null if not updated
            Long updateTime = getRecordLongValue(record, "DateModified", this.user);
            updateTime = updateTime == null ? startTime : updateTime;

            if(startTime != null && (startTime < this.startTime || this.startTime == 0L)){
                // Update startTime if valid and less than sample's initialized startTime
                this.startTime = startTime;
            }
            if(updateTime != null && updateTime > this.updateTime){
                this.updateTime = updateTime;
            }

            this.totalSamples += 1;

            DataRecord[] children = new DataRecord[0];
            try {
                children = record.getChildrenOfType("Sample", this.user);
            } catch (IoError | RemoteException e){
                log.error(String.format("Failed to get children from Record: %d", record.getRecordId()));
            }

            if(children.length > 0){
                // The sample moved on in the process
                this.completedSamples += 1;
                Set<String> childStatuses = Stream.of(children)
                        .map(sample -> getRecordStringValue(sample, "ExemplarSampleStatus", this.user))
                        .collect(Collectors.toSet());
                this.nextSteps.addAll(childStatuses);
            } else {
                // Sample hasn't moved on and is not complete unless it has completed Illumina Sequencing
                if(!recordStatusIsComplete(recordStatus)){
                    this.complete = false;
                }
            }
        }

        private boolean recordStatusIsComplete(String status){
            return status.toLowerCase().contains("completed - illumina sequencing");
        }

        public Map<String, Object> jsonify(){
            Map<String, Object> json = new HashMap<>();
            json.put("step", this.step);
            json.put("complete", this.complete);
            json.put("totalSamples", this.totalSamples);
            json.put("completedSamples", this.completedSamples);
            json.put("next", this.nextSteps);
            return json;
        }
    }

    final class Node<K, V> implements Map.Entry<K, V> {
        private final K key;
        private V value;

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return old;
        }
    }

    // TODO - Maybe accept a String[] fields array that is all the fields that should be pulled from a sample
    public Map<String, Object> execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();
            List<DataRecord> requestRecord = drm.queryDataRecords("Request", "RequestId = '" + this.requestId + "'", user);
            if (requestRecord.size() != 1) {  // error: request ID not found or more than one found
                log.error("Request not found:" + requestId);
                return new HashMap<>();
            }

            // Immediate samples of the request. These samples represent the overall progress of each project sample
            DataRecord[] samples = requestRecord.get(0).getChildrenOfType("Sample", user);
            List<SampleTracker> sampleTrackers = Stream.of(samples)
                    .map(sample -> new SampleTracker(sample, user))
                    .collect(Collectors.toList());

            // Do a BFS using a reference to the sampleTrackers
            List<Node<DataRecord, SampleTracker>> queue = sampleTrackers.stream()
                    .map(tracker -> (Node<DataRecord, SampleTracker>) new Node(tracker.record, tracker))
                    .collect(Collectors.toList());
            while(queue.size() > 0){
                Node<DataRecord, SampleTracker> node = queue.remove(0);
                SampleTracker tracker = node.getValue();
                DataRecord parent = node.getKey();

                DataRecord[] children = parent.getChildren(user);
                for(DataRecord record : children){
                    tracker.addSample(record);
                    Node<DataRecord, SampleTracker> child = new Node<>(record, tracker);
                    queue.add(child);
                }
            }

            RequestTracker requestTracker = new RequestTracker(sampleTrackers);
            return requestTracker.toApiResponse();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
