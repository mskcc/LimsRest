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
import java.util.stream.Collectors;

public class GetRequestTrackingTask {
    private static Log log = LogFactory.getLog(GetRequestTrackingTask.class);

    private ConnectionLIMS conn;
    private String requestId;

    private static String[] FIELDS = new String[] {"ExemplarSampleStatus", "Recipe"};

    public GetRequestTrackingTask(String requestId, ConnectionLIMS conn) {
        this.requestId = requestId;
        this.conn = conn;
    }

    private class Node {
        private DataRecord record;
        private DataRecord currRecord;
        private List<Map<String, String>> trace; // List of statuses
        private User user;

        public long getRecordId(){
            return record.getRecordId();
        }

        public List<Map<String, String>> getTrace(){
            return this.trace;
        }

        /**
         * Node is created only for a sample
         *
         * @param user
         * @param record
         */
        public Node(User user, DataRecord record){
            this.user = user;
            this.record = record;
            this.currRecord = record;
            this.trace = new ArrayList<>();
        }

        public void setCurrentRecord(DataRecord record){
            this.currRecord = record;
        }


        /**
         * Adds a status object to the current trace
         *
         * @param status
         */
        public void addStatus(Map<String, String> status){
            this.trace.add(status);
        }



        /**
         * Used to create a child from this node. Clones this node and appends new info to the trace
         *
         * @param record
         * @return
         */
        public Node createChild(DataRecord record){
            // Preserve the original sample record
            Node child = new Node(user, this.record);
            child.setCurrentRecord(record);

            // Provide the furthest trace of samples
            for(Map<String, String> pathInfo : trace){
                child.addStatus(pathInfo);
            }

            // Add Record info for the child record
            Map<String, String> sampleInfo = new HashMap<>();
            sampleInfo.put("RecordId",  Long.toString(record.getRecordId()));
            for(String key : FIELDS){
                try {
                    sampleInfo.put(key, record.getStringVal(key, user));
                } catch (NotFound | RemoteException e){
                    log.warn(String.format("Unable to extract key: %s, from record: %d", key, record.getRecordId()));
                }
            }
            child.addStatus(sampleInfo);

            return child;
        }

        /**
         * Returns all children of the DataRecord
         *
         * @return
         */
        public DataRecord[] getChildren(){
            try {
                return this.currRecord.getChildrenOfType("Sample", this.user);
            } catch(IoError | RemoteException e){
                log.error(String.format("Failed to grab children of record Id: %d", this.record.getRecordId()));
                return new DataRecord[0];
            }
        }

        /**
         * Returns a Map object of the tracked information
         *
         * @return
         */
        public Map<String, Object> getTrackingMap(){
            Map<String, Object> trackingMap = new HashMap<>();
            trackingMap.put("SampleId", this.record.getRecordId());
            trackingMap.put("trace", this.trace);

            return trackingMap;
        }
    }

    private class SampleTracker {
        Long sampleId;
        List<List<Map<String, String>>> samplePaths;

        public SampleTracker(Long sampleId){
            this.sampleId = sampleId;
            this.samplePaths = new ArrayList<>();
        }
        public void addPath(List<Map<String, String>> path){
            this.samplePaths.add(path);
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

        private class Step {
            public String step;
            public boolean complete;
            public int totalSamples;
            public int completedSamples;
            public Set<String> nextSteps;       // Exemplar statuses

            public Step(String step){
                this.step = step;
                this.complete = false;
                this.totalSamples = 0;
                this.completedSamples = 0;
                nextSteps = new HashSet<>();
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

        private Map<String, Step> processPaths(List<List<Map<String, String>>> paths){
            int idx = 0;
            List<List<Map<String, String>>> remainingPaths = getRemainingPaths(paths, 0);

            // Status -> StepTracker
            Map<String, Step> steps = new HashMap<>();

            while(remainingPaths.size() > 0){
                for(List<Map<String, String>> path : remainingPaths){
                    Map<String, String> step = path.get(idx);
                    String status = step.get("ExemplarSampleStatus");
                    Step stepTracker;
                    if(steps.containsKey(status)){
                        stepTracker = steps.get(status);
                    } else {
                        stepTracker = new Step(status);
                        steps.put(status, stepTracker);
                    }
                    stepTracker.totalSamples += 1;

                    int nextIdx = idx+1;
                    // Check if there's a next step
                    if(path.size() > nextIdx){
                        stepTracker.completedSamples += 1;  // If there's a next step, this step has been completed
                        stepTracker.nextSteps.add(path.get(nextIdx).get("ExemplarSampleStatus"));
                    }
                }

                remainingPaths = getRemainingPaths(remainingPaths, ++idx);
            }

            for(Map.Entry<String, Step> entry : steps.entrySet()){
                Step step = entry.getValue();
                if(step.completedSamples == step.totalSamples){
                    step.complete = true;
                } else {
                    step.complete = false;
                }
            }

            return steps;
        }

        private List<Map<String,Object>> jsonifySampleSteps(Map<String, Step> steps){
            List<Map<String,Object>> jsonSteps = steps.values().stream().map(step -> step.jsonify()).collect(Collectors.toList());
            return jsonSteps;
        }

        public Map<String, Object> toApiEntry(){
            Map<String, Object> apiEntry = new HashMap<>();
            apiEntry.put("sampleId", this.sampleId);
            apiEntry.put("samplePaths", this.samplePaths);

            Map<String, Step> processedPaths = processPaths(this.samplePaths);
            List<String> completedSteps = processedPaths.values()
                    .stream()
                    .filter(step -> step.complete)
                    .map(step -> step.step)
                    .collect(Collectors.toList());
            List<String> pendingSteps = processedPaths.values()
                    .stream()
                    .filter(step -> !step.complete)
                    .map(step -> step.step)
                    .collect(Collectors.toList());

            apiEntry.put("steps", jsonifySampleSteps(processedPaths));
            apiEntry.put("complete", pendingSteps.size() == 0);
            apiEntry.put("pendingSteps", pendingSteps);
            apiEntry.put("completedSteps", completedSteps);

            // TODO - Add Status/Steps for each one

            return apiEntry;
        }
    }

    // TODO - Maybe accept a String[] fields array that is all the fields that should be pulled from a sample
    public RequestTrackerModel execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();

            List<DataRecord> requestRecord = drm.queryDataRecords("Request", "RequestId = '" + this.requestId + "'", user);
            if (requestRecord.size() != 1) {  // error: request ID not found or more than one found
                log.error("Request not found:" + requestId);
                return new RequestTrackerModel(null, requestId); // SampleTrackingList();
            }

            // Immediate samples of the request. These samples represent the overall progress of each project sample
            DataRecord[] samples = requestRecord.get(0).getChildrenOfType("Sample", user);
            Map<Long, SampleTracker> sampleTrackingMap = new HashMap<>();
            for(DataRecord sample : samples){
                Long recordId = sample.getRecordId();
                sampleTrackingMap.put(recordId, new SampleTracker(recordId));
            }

            // Perform BFS from the samples to find all "sub-samples" that represent the sample at different stages
            List<Node> stack = Arrays.stream(samples)
                                            .map(record -> new Node(user, record))
                                            .collect(Collectors.toList());
            while(stack.size() > 0){
                Node parent = stack.remove(0);
                DataRecord[] children = parent.getChildren();
                if(children.length > 0){
                    List<Node> childNodes = Arrays.stream(children)
                                                         .map(child -> parent.createChild(child))
                                                         .collect(Collectors.toList());
                    stack.addAll(childNodes);
                } else {
                    // Record the sample Trace
                    long sampleId = parent.getRecordId();
                    sampleTrackingMap.get(sampleId).addPath(parent.getTrace());
                }
            }

            List<Map<String, Object>> projectSamples = sampleTrackingMap.values().stream()
                                                        .map(tracker -> tracker.toApiEntry())
                                                        .collect(Collectors.toList());

            return new RequestTrackerModel(projectSamples, this.requestId);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
