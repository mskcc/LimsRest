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
import org.mskcc.limsrest.service.requesttracker.*;
import org.mskcc.limsrest.util.Pair;

import java.rmi.RemoteException;
import java.security.Key;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mskcc.limsrest.service.requesttracker.StatusTrackerConfig.isCompletedStatus;
import static org.mskcc.limsrest.util.DataRecordAccess.getRecordBooleanValue;
import static org.mskcc.limsrest.util.DataRecordAccess.getRecordStringValue;

public class GetRequestTrackingTask {
    private static Log log = LogFactory.getLog(GetRequestTrackingTask.class);

    private ConnectionLIMS conn;
    private String requestId;
    private String serviceId;

    private static String[] FIELDS = new String[] {"ExemplarSampleStatus", "Recipe"};
    private static String[] DATE_FIELDS = new String[] {"DateCreated", "DateModified"};

    public GetRequestTrackingTask(String requestId, String serviceId, ConnectionLIMS conn) {
        this.requestId = requestId;
        this.serviceId = serviceId;
        this.conn = conn;
    }

    private List<DataRecord> getBankedSampleRecords(String serviceId, User user, DataRecordManager drm){
        String query = String.format("ServiceId = '%s'", serviceId);
        List<DataRecord> bankedList = new ArrayList<>();
        try {
             bankedList = drm.queryDataRecords("BankedSample", query, user);
        } catch (NotFound | IoError | RemoteException e){
            log.info(String.format("Could not find BankedSample record for %s", serviceId));
            return null;
        }
        return bankedList;
    }

    /**
     * Returns map of all IGO requestIDs for a bankedSample service ID and their corresponding submitted Status
     *
     * @param serviceId
     * @param user
     * @param drm
     * @return Map {
     *     requestID: Stage,
     *     ...
     * }
     */
    private Map<String, Stage> getSubmittedStages(String serviceId, User user, DataRecordManager drm) {
        List<DataRecord> bankedSampleRecords = getBankedSampleRecords(serviceId, user, drm);

        // Validate serviceId
        if(bankedSampleRecords.isEmpty()){
            throw new IllegalArgumentException(String.format("Invalid serviceID: %s", this.serviceId));
        }

        Map<String, Map<String, Integer>> requestMap = new HashMap<>();
        Boolean wasPromoted;
        String requestId;
        for(DataRecord record : bankedSampleRecords){
            requestId = getRecordStringValue(record, "RequestId", user);
            wasPromoted = getRecordBooleanValue(record, "Promoted", user);

            // Grab/Create the entry for the given request
            Map<String, Integer> requestEntry = requestMap.computeIfAbsent(requestId, k -> new HashMap<String, Integer>());
            Integer promoted = requestEntry.computeIfAbsent("Promoted", k -> 0);
            Integer total = requestEntry.computeIfAbsent("Total", k -> 0);

            requestEntry.put("Total", total +1);
            if(wasPromoted){
                // Check if was promoted
                requestEntry.put("Promoted",  promoted+1);
            }
        }

        Map<String, Stage> stageMap = new HashMap<>();
        Map<String, Integer> sampleCountMap;
        int inputSampleCount;
        int promotedSampleCount;
        for(Map.Entry<String, Map<String, Integer>> entry : requestMap.entrySet()){
            sampleCountMap = entry.getValue();
            requestId = entry.getKey();
            promotedSampleCount = sampleCountMap.get("Promoted");
            inputSampleCount = sampleCountMap.get("Total");

            Stage requestStage = new Stage("submitted", inputSampleCount, promotedSampleCount, null, null);
            stageMap.put(requestId, requestStage);

            Step receivedStep = new Step("received", null);
            receivedStep.setComplete(true);                         // automatically true - each input is complete
            receivedStep.setTotalSamples(inputSampleCount);
            receivedStep.setCompletedSamples(inputSampleCount);

            Step promotedStep = new Step("promoted", null);
            receivedStep.setComplete(inputSampleCount == promotedSampleCount);  // Complete if all samples promoted
            receivedStep.setTotalSamples(inputSampleCount);                     // Total:       # of received samples
            receivedStep.setCompletedSamples(promotedSampleCount);              // Complete:    # of promoted samples

            requestStage.addStep(receivedStep);
            requestStage.addStep(promotedStep);
        }

        /*
        Set<String> uniqueRequestIds = bankedSampleRecords.stream()
                .map(record ->
                .collect(Collectors.toSet());
        List<String> requestIds = new ArrayList<>(uniqueRequestIds);

        Integer bankedCount =  bankedSampleRecords.size();
        Integer promotedCount = promotedRecords.size();

        Map<String, Object> receivedStep = new HashMap<>();
        receivedStep.put("step", "Received");
        receivedStep.put("complete", Boolean.TRUE);
        receivedStep.put("totalSamples", bankedCount);
        receivedStep.put("completedSamples", bankedCount);

        Map<String, Object> promotedStep = new HashMap<>();
        promotedStep.put("step", "Promoted");
        promotedStep.put("complete", bankedCount == promotedCount); // Complete if all promoted
        promotedStep.put("totalSamples", bankedCount);
        promotedStep.put("completedSamples", promotedCount);

        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(receivedStep);
        steps.add(promotedStep);

        Map<String, Object> stage = new HashMap<>();
        stage.put("stage", "Submitted");
        stage.put("serviceId", serviceId);
        stage.put("inputCount", bankedCount);
        stage.put("outputCount", promotedCount);
        stage.put("steps", steps);
        */
        return stageMap;
    }

    private boolean sampleFailed(String status){
        return status.toLowerCase().contains("failed");
    }

    /**
     * Finds all samples w/o children that have not been failed
     *
     * @param sample
     * @param paths
     * @param user
     * @return
     */
    private List<Sample> findValidLeafSamples(Sample sample, List<Sample> paths, User user){
        DataRecord[] children = new DataRecord[0];

        DataRecord record = sample.getRecord();

        try {
            children = record.getChildrenOfType("Sample", user);
        } catch (IoError | RemoteException e){
            // TODO
        }

        if(children.length == 0){
            // Terminating node is reached
            paths.add(sample);
            return paths;
        } else {
            for(DataRecord childRecord : children){
                Sample childSample = new Sample(childRecord, user);
                /*
                Sample childSample = new Sample(child.getRecordId(), getRecordStringValue(child, "ExemplarSampleStatus", user));
                childSample.setRecord(child);
                 */
                childSample.setParent(sample);
                paths.addAll(findValidLeafSamples(childSample, new ArrayList<>(), user));
            }
        }

        return paths;
    }

    /**
     * Calculate the stage the overall sample is at based on the route one path
     *
     * @param path
     */
    public Map<String, Stage> calculateSampleStage(List<? extends Tracker> path) {
        Map<String, Stage> stageMap = new HashMap<>();
        if(path.size() == 0) return stageMap;

        ListIterator<? extends Tracker> itr = path.listIterator(path.size()); // path.iterator();
        Tracker tracker = itr.previous();

        String stageName = tracker.getStage();
        Long startTime = tracker.getStartTime();
        Long updateTime = tracker.getUpdateTime();
        Stage stage = new Stage(stageName, tracker.getSize(), 0, startTime, updateTime);

        // Stage can only be set to incomplete if it is the last Stage and doesn't have a complete status
        stage.setComplete(tracker.getComplete());
        stageMap.put(stageName, stage);
        while (itr.hasPrevious()) {
            tracker = itr.previous();

            stageName = tracker.getStage();
            startTime = tracker.getStartTime();
            updateTime = tracker.getUpdateTime();

            if(stageMap.containsKey(stageName)){
                stage = stageMap.get(stageName);

                // Update the start & end times of the stage
                stage.addStartingSample(tracker.getSize());
                stage.addEndingSample(tracker.getSize());
                if (stage.getStartTime() > startTime) {
                    stage.setStartTime(startTime);
                }
                if (stage.getUpdateTime() < updateTime) {
                    stage.setUpdateTime(updateTime);
                }
            } else {
                // Add a new stage, which must be complete if it preceded another entry
                stageMap.put(stageName, new Stage(stageName, tracker.getSize(), tracker.getSize(), startTime, updateTime));
                stage.setComplete(Boolean.TRUE);
            }
        }

        return stageMap;
    }

    // TODO - Maybe accept a String[] fields array that is all the fields that should be pulled from a sample
    public Map<String, Map<String, Object>>  execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();

            // Find all RequestIds for a serviceId and grab Stage Information from those requestIds
            // ServiceIds -(1:many)-> IGO Request ID
            Map<String, Stage> submittedStages = getSubmittedStages(this.serviceId, user, drm);
            List<String> requestIds = new ArrayList<>(submittedStages.keySet());

            Map<String, Stage> sampleQcStages = new HashMap<>();
            Map<String, Stage> libraryPrepStages = new HashMap<>();
            Map<String, Stage> sequencingStages = new HashMap<>();
            Map<String, Stage> dataQcStages = new HashMap<>();
            Map<String, Stage> igoCompleteStages = new HashMap<>();


            Map<String, Request> requestMap = new HashMap<>();
            for(String requestId : requestIds){
                requestMap.putIfAbsent(requestId, new Request(requestId, serviceId));
                Request request = requestMap.get(requestId);

                List<DataRecord> requestRecordList = drm.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
                if (requestRecordList.size() != 1) {  // error: request ID not found or more than one found
                    log.error(String.format("Request not found: %s. Returning incomplete information", requestId));
                }


                DataRecord requestRecord = requestRecordList.get(0);

                // TODO - Get all relevant Request information

                try {
                    request.setReceivedDate(requestRecord.getLongVal("ReceivedDate", user));
                    Long recentDeliveryDate = requestRecord.getLongVal("RecentDeliveryDate", user);
                    if(recentDeliveryDate != null){
                        // Projects are considered IGO-Complete if they have a delivery date
                        request.setDeliveryDate(recentDeliveryDate);
                        request.setIgoComplete(true);
                    }
                } catch (NullPointerException e){
                    // This is an expected exception when DataRecord fields have not been set
                }

                // Immediate samples of the request. These samples represent the overall progress of each project sample
                DataRecord[] samples = requestRecord.getChildrenOfType("Sample", user);

                // Find paths & calculate stages for each sample in the request
                for(DataRecord record : samples){
                    SampleTracker tracker = new SampleTracker(record, user);

                    Sample parentSample = new Sample(record, user);

                    parentSample.setRecord(record);
                    // Finds all non-failed leaf samples
                    List<Sample> leafSamples = findValidLeafSamples(parentSample, new ArrayList<>(), user);

                    // If there are no leaf-samples, the sample has failed
                    tracker.setFailed(leafSamples.size() == 0);

                    // Find all paths from leaf to parent nodes
                    List<List<Sample>> paths = new ArrayList<>();
                    for(Sample curr : leafSamples){
                        List<Sample> path = new ArrayList<>();

                        while(curr != null){
                            curr.enrichSample();        // Add extra data fields to sample
                            // Samples are complete because they have a sample after teh
                            curr.setComplete(Boolean.TRUE);
                            path.add(curr);
                            curr = curr.getParent();
                        }

                        // Reset the leaf-node complete status based off of its status
                        Sample leaf = path.get(0);
                        leaf.setComplete(isCompletedStatus(leaf.getStatus()));

                        Collections.reverse(path); // Get path samples in order from parent to their leaf samples
                        paths.add(path);
                    }
                    tracker.setPaths(paths);

                    // Determine stages of the sampleTracker
                    for(List<Sample> path : paths){
                        tracker.setStages(calculateSampleStage(path));
                        // tracker.calculateSampleStage(path);
                    }

                    request.addSampleTracker(tracker);
                }

                // Aggregate sample stage information into the request level




                Map<String, Map<String, Object>> apiResponse = new HashMap<>();
                for(Map.Entry<String, Request> entry : requestMap.entrySet()){
                    apiResponse.put(entry.getKey(), entry.getValue().toApiResponse());
                }

                return apiResponse;

                /*
                // Do a BFS using a reference to the sampleTrackers
                List<Pair<DataRecord, SampleTracker>> queue = sampleTrackers.stream()
                        .map(tracker -> (Pair<DataRecord, SampleTracker>) new Pair(tracker.getRecord(), tracker))
                        .collect(Collectors.toList());
                while(queue.size() > 0){
                    Pair<DataRecord, SampleTracker> node = queue.remove(0);
                    SampleTracker tracker = node.getValue();
                    DataRecord parent = node.getKey();

                    DataRecord[] children = parent.getChildren(user);

                    // TODO - This should only be for an admin view
                    // Add all child Records for the parent to the sampleTracker
                    List<Long> childRecordIds = Arrays.stream(children)
                            .map(record -> record.getRecordId())
                            .collect(Collectors.toList());
                    tracker.addChildrenToRecord(parent.getRecordId(), childRecordIds);

                    for(DataRecord record : children){
                        tracker.addSample(record);
                        Pair<DataRecord, SampleTracker> child = new Pair<>(record, tracker);
                        queue.add(child);
                    }
                }

                RequestTracker requestTracker = new RequestTracker(sampleTrackers);
                Map<String, Object> trackerResponse = requestTracker.toApiResponse();

                Map<String, Object> stagesMap = new HashMap<>();
                trackerResponse.put("stages", stagesMap);

                stagesMap.put("submitted", submittedStage);


                sampleQcStages.put(requestId, getSampleQcStages());
                libraryPrepStages.put(requestId, getLibraryPrepStages());
                sequencingStages.put(requestId, getSequencingStages());
                dataQcStages.put(requestId, getDataQcStages());
                igoCompleteStages.put(requestId, getIgoCompleteStages());
                 */
            }


            // TODO - MAYBE? Allow for just requestID to be sent

            /*
            // Only get Submitted Stage if a serviceId is provided
            // ServiceIds -(1:many)-> IGO Request ID
            Map<String, Object> submittedStage = new HashMap<>();
            List<String> requestIds = new ArrayList<>(Arrays.asList(this.requestId));
            if(this.serviceId != null){

                Set<String> uniqueRequestIds = bankedSampleRecords.stream()
                        .map(record -> getRecordStringValue(record, "RequestId", user))
                        .collect(Collectors.toSet());
                requestIds = new ArrayList<>(uniqueRequestIds);
            }
             */



            /*
            List<DataRecord> requestRecordList = drm.queryDataRecords("Request", "RequestId = '" + this.requestId + "'", user);
            if (requestRecordList.size() != 1) {  // error: request ID not found or more than one found
                log.error("Request not found:" + requestId);
                return new HashMap<>();
            }

            // Get all relevant Request information
            DataRecord requestRecord = requestRecordList.get(0);
            Long receivedDate = null;
            Long deliveryDate = null;
            // TODO - exception is thrown if these do not exist
            try {
                receivedDate = requestRecord.getLongVal("ReceivedDate", user);
                deliveryDate = requestRecord.getLongVal("RecentDeliveryDate", user);
            } catch (NullPointerException e){
                // This is an expected exception
            }

            // Immediate samples of the request. These samples represent the overall progress of each project sample
            DataRecord[] samples = requestRecord.getChildrenOfType("Sample", user);
            List<SampleTracker> sampleTrackers = Stream.of(samples)
                    .map(sample -> new SampleTracker(sample, user))
                    .collect(Collectors.toList());

            // Do a BFS using a reference to the sampleTrackers
            List<Pair<DataRecord, SampleTracker>> queue = sampleTrackers.stream()
                    .map(tracker -> (Pair<DataRecord, SampleTracker>) new Pair(tracker.getRecord(), tracker))
                    .collect(Collectors.toList());
            while(queue.size() > 0){
                Pair<DataRecord, SampleTracker> node = queue.remove(0);
                SampleTracker tracker = node.getValue();
                DataRecord parent = node.getKey();

                DataRecord[] children = parent.getChildren(user);
                for(DataRecord record : children){
                    tracker.addSample(record);
                    Pair<DataRecord, SampleTracker> child = new Pair<>(record, tracker);
                    queue.add(child);
                }
            }

            RequestTracker requestTracker = new RequestTracker(sampleTrackers, deliveryDate, receivedDate);
            Map<String, Object> trackerResponse = requestTracker.toApiResponse();

            Map<String, Object> stagesMap = new HashMap<>();
            trackerResponse.put("stages", stagesMap);

            stagesMap.put("submitted", submittedStage);

            return trackerResponse;
             */

            return new HashMap<>();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /*
    private Stage getIgoCompleteStages() {
    }

    private Stage getDataQcStages() {
    }

    private Stage getSequencingStages() {
    }

    private Stage getLibraryPrepStages() {
    }

    private Stage getSampleQcStages() {
    }

     */
}
