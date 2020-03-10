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

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

import static org.mskcc.limsrest.service.requesttracker.StatusTrackerConfig.*;
import static org.mskcc.limsrest.util.DataRecordAccess.*;

public class GetRequestTrackingTask {
    private static Log log = LogFactory.getLog(GetRequestTrackingTask.class);

    private ConnectionLIMS conn;
    private String requestId;
    private String serviceId;

    private static String[] FIELDS = new String[] {"ExemplarSampleStatus", "Recipe"};
    private static String[] DATE_FIELDS = new String[] {"DateCreated", "DateModified"};

    // LIMS fields for the request metadata - separated by string & long value types
    private static String[] requestDataLongFields = new String[] { "RecentDeliveryDate", "ReceivedDate" };
    private static String[] requestDataStringFields = new String[] {
            "LaboratoryHead",
            "IlabRequest",
            "GroupLeader",
            "TATFromInProcessing",
            "TATFromReceiving",
            "ProjectManager",
            "LabHeadEmail",
            "Investigator"
    };


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
    private Map<String, SampleStageTracker> getSubmittedStages(String serviceId, User user, DataRecordManager drm) {
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

        Map<String, SampleStageTracker> stageMap = new HashMap<>();
        Map<String, Integer> sampleCountMap;
        int inputSampleCount;
        int promotedSampleCount;
        for(Map.Entry<String, Map<String, Integer>> entry : requestMap.entrySet()){
            sampleCountMap = entry.getValue();
            requestId = entry.getKey();
            promotedSampleCount = sampleCountMap.get("Promoted");
            inputSampleCount = sampleCountMap.get("Total");

            SampleStageTracker requestStage = new SampleStageTracker("submitted", inputSampleCount, promotedSampleCount, null, null);
            if(inputSampleCount == promotedSampleCount){
                // Request is complete if all samples have been promoted
                requestStage.setComplete(Boolean.TRUE);
            }
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
    private List<AliquotStageTracker> findValidLeafSamples(AliquotStageTracker sample, List<AliquotStageTracker> paths, User user){
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
                AliquotStageTracker childSample = new AliquotStageTracker(childRecord, user);
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
    public Map<String, SampleStageTracker> calculateStages(List<? extends StageTracker> path, boolean aggregate) {
        Integer sampleSize = 1; // A sample will have a size of 1

        Map<String, SampleStageTracker> stageMap = new TreeMap<>(new StatusTrackerConfig.StageComp());
        if(path.size() == 0) return stageMap;

        Iterator<? extends StageTracker> itr = path.iterator();
        StageTracker stageTracker;
        String stageName;
        SampleStageTracker stage;
        while (itr.hasNext()) {
            stageTracker = itr.next();
            stageName = stageTracker.getStage();
            if(stageMap.containsKey(stageName)){
                stage = stageMap.get(stageName);
                if(aggregate){
                    stage.addStartingSample(sampleSize);
                }
                stage.updateStage(stageTracker);
            } else {
                stage = new SampleStageTracker(stageName, sampleSize, 0, stageTracker.getStartTime(), stageTracker.getUpdateTime());
                stageMap.put(stageName, stage);
            }
        }

        stageMap.forEach((String k, SampleStageTracker v) -> {
            String nextStage = getNextStage(v.getStage());
            SampleStageTracker tracker = stageMap.get(nextStage);
            if(tracker != null){
                v.addEndingSample(tracker.getSize());
                if(v.getEndingSamples() == v.getSize()){
                    v.setComplete(Boolean.TRUE);
                } else {
                    v.setComplete(Boolean.FALSE);
                }
            }
        });
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
            Map<String, SampleStageTracker> submittedStages = getSubmittedStages(this.serviceId, user, drm);
            List<String> requestIds = new ArrayList<>(submittedStages.keySet());

            Map<String, Request> requestMap = new HashMap<>();
            for(String requestId : requestIds) {
                requestMap.putIfAbsent(requestId, new Request(requestId, serviceId));
                Request request = requestMap.get(requestId);
                request.addStage("submitted", submittedStages.get(requestId));

                List<DataRecord> requestRecordList = drm.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
                if (requestRecordList.size() != 1) {  // error: request ID not found or more than one found
                    log.error(String.format("Request not found: %s. Returning incomplete information", requestId));
                }

                DataRecord requestRecord = requestRecordList.get(0);

                Map<String, Object> requestMetaData = new HashMap<>();
                for(String field : requestDataStringFields){
                    requestMetaData.put(field, getRecordStringValue(requestRecord, field, user));
                }
                for(String field : requestDataLongFields){
                    requestMetaData.put(field, getRecordLongValue(requestRecord, field, user));
                }
                request.setMetaData(requestMetaData);

                if (getRecordLongValue(requestRecord, "RecentDeliveryDate", user) != null) {
                    // Projects are considered IGO-Complete if they have a delivery date
                    request.setIgoComplete(true);
                }

                // Immediate samples of the request. These samples represent the overall progress of each project sample
                DataRecord[] samples = requestRecord.getChildrenOfType("Sample", user);

                // Find paths & calculate stages for each sample in the request
                for (DataRecord record : samples) {
                    SampleTracker tracker = new SampleTracker(record, user);

                    // Finds all non-failed leaf samples
                    AliquotStageTracker parentSample = new AliquotStageTracker(record, user);
                    parentSample.setRecord(record);
                    List<AliquotStageTracker> leafSamples = findValidLeafSamples(parentSample, new ArrayList<>(), user);

                    // If there are no leaf-samples, the sample has failed
                    tracker.setFailed(leafSamples.size() == 0);

                    // Find all paths from leaf to parent nodes
                    List<List<AliquotStageTracker>> paths = new ArrayList<>();
                    for (AliquotStageTracker curr : leafSamples) {
                        List<AliquotStageTracker> path = new ArrayList<>();

                        while (curr != null) {
                            curr.enrichSample();        // Add extra data fields to sample

                            // If this is unknown, use the last element of the path
                            if(curr.getStage() == STAGE_UNKNOWN && path.size() > 0){
                                curr.setStage(path.get(path.size()-1).getStage());
                            }

                            // Samples are complete because they have a sample after them
                            curr.setComplete(Boolean.TRUE);
                            path.add(curr);
                            curr = curr.getParent();
                        }

                        // Reset the leaf-node complete status based off of its status
                        AliquotStageTracker leaf = path.get(0);
                        leaf.setComplete(isCompletedStatus(leaf.getStatus()));

                        Collections.reverse(path); // Get path samples in order from parent to their leaf samples
                        paths.add(path);
                    }
                    tracker.setPaths(paths);

                    // Determine stages of the sampleTracker based on the paths for that sample
                    for (List<AliquotStageTracker> path : paths) {
                        Map<String, SampleStageTracker> aliquotStages = calculateStages(path, false);
                        tracker.addStage(aliquotStages);
                    }

                    Optional<Boolean> isComplete = tracker.getStages().values().stream().map(stage -> stage.getComplete()).reduce(Boolean::logicalAnd);
                    if(isComplete.isPresent()){
                        tracker.setComplete(isComplete.get());
                    } else {
                        tracker.setComplete(Boolean.FALSE);
                    }

                    request.addSampleTracker(tracker);
                }

                // Aggregate sample stage information into the request level
                List<SampleTracker> sampleTrackers = request.getSamples();
                List<SampleStageTracker> requestStages = sampleTrackers.stream()
                        .flatMap(tracker -> tracker.getStages().values().stream())
                        .collect(Collectors.toList());
                Map<String, SampleStageTracker> requestStagesMap = calculateStages(requestStages, true);

                // Determine stages of the sampleTracker
                for (Map.Entry<String, SampleStageTracker> requestStage : requestStagesMap.entrySet()) {
                    request.addStage(requestStage.getKey(), requestStage.getValue());
                }
            }

            // JSONIY
            Map<String, Map<String, Object>> apiResponse = new HashMap<>();
            for (Map.Entry<String, Request> entry : requestMap.entrySet()) {
                apiResponse.put(entry.getKey(), entry.getValue().toApiResponse());
            }

            return apiResponse;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
