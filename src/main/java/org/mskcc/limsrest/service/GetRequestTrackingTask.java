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


    public GetRequestTrackingTask(String requestId, ConnectionLIMS conn) {
        this.requestId = requestId;
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
                childSample.enrichSample();
                paths.addAll(findValidLeafSamples(childSample, new ArrayList<>(), user));
            }
        }

        return paths;
    }

    /**
     * Calculates the stage the overall sample is at based on the least advanced path
     *
     * @param path - Path of Aliquot/Sample trackers
     * @param aggregate - Should the size of each sample in the path be aggregated (Aliquots: False, Samples: True)
     * @return
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

    private SampleStageTracker getBankedSampleStage(String serviceId, User user, DataRecordManager drm){
        Map<String, Integer> tracker = new HashMap<>();

        String query = String.format("ServiceId = '%s'", serviceId);
        List<DataRecord> bankedList = new ArrayList<>();
        try {
            bankedList = drm.queryDataRecords("BankedSample", query, user);
        } catch (NotFound | IoError | RemoteException e){
            log.info(String.format("Could not find BankedSample record for %s", serviceId));
            return null;
        }

        Boolean wasPromoted;
        Integer total;
        Integer promoted;
        for(DataRecord record : bankedList){
            // Increment total
            total = tracker.computeIfAbsent("Total", k -> 0);
            tracker.put("Total", total +1);

            // Increment promoted if sample was promoted
            promoted = tracker.computeIfAbsent("Promoted", k -> 0);
            wasPromoted = getRecordBooleanValue(record, "Promoted", user);
            if(wasPromoted){
                tracker.put("Promoted",  promoted+1);
            }
        }

        total = tracker.get("Total");
        promoted = tracker.get("Promoted");

        SampleStageTracker requestStage = new SampleStageTracker("submitted", total, promoted, null, null);
        Boolean submittedComplete = total == promoted;
        if(submittedComplete){
            requestStage.setComplete(Boolean.TRUE);
        }

        Step receivedStep = new Step("received", null);
        receivedStep.setComplete(true);                         // automatically true - each input is complete
        receivedStep.setTotalSamples(total);
        receivedStep.setCompletedSamples(total);

        Step promotedStep = new Step("promoted", null);
        receivedStep.setComplete(submittedComplete);  // Complete if all samples promoted
        receivedStep.setTotalSamples(total);                     // Total:       # of received samples
        receivedStep.setCompletedSamples(promoted);              // Complete:    # of promoted samples

        requestStage.addStep(receivedStep);
        requestStage.addStep(promotedStep);

        return requestStage;
    }

    public String getBankedSampleServiceId(String requestId, User user, DataRecordManager drm) {
        String query = String.format("RequestId = '%s'", requestId);
        List<DataRecord> bankedList = new ArrayList<>();
        try {
            bankedList = drm.queryDataRecords("BankedSample", query, user);
        } catch (NotFound | IoError | RemoteException e){
            log.info(String.format("Could not find BankedSample records w/ RequestId: %s", requestId));
            return null;
        }

        Set<String> serviceIds = new HashSet<>();
        String serviceId;
        for(DataRecord record : bankedList){
            serviceId = getRecordStringValue(record, "ServiceId", user);
            serviceIds.add(serviceId);
        }

        if(serviceIds.size() != 1){
            String error = String.format("Failed to retrieve serviceId for RequestId: %s. ServiceIds Returned: %s",
                    requestId, serviceIds.toString());
            log.error(error);
            return "";
        }

        return new ArrayList<String>(serviceIds).get(0);
    }

    /**
     * Calculates the leaf status & stage, which is dependent on determination of the sample's path.
     *  Special logic for:
     *      - Failed Status
     *      - Stage
     *      - Status
     * @param leaf - Tracker that is the leaf of a path
     */
    private void enrichLeafSample(AliquotStageTracker leaf){
        leaf.setComplete(isCompletedStatus(leaf.getStatus()));
        if(leaf.getFailed()){
            // If the leaf node is failed, set the stage based off of the parent
            leaf.setStage(leaf.getParent().getStage());
        }
    }

    // TODO - Maybe accept a String[] fields array that is all the fields that should be pulled from a sample
    public Map<String, Object> execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();

            String serviceId = getBankedSampleServiceId(this.requestId, user, drm);

            Request request = new Request(requestId, serviceId);

            if(serviceId != null && !serviceId.equals("")){
                // Add "submitted" stage if a serviceID exists
                // TODO - Why is this the case (QA: "06302_W")
                // TODO - Place in thread as this can be executed independently
                SampleStageTracker submittedStage = getBankedSampleStage(serviceId, user, drm);
                request.addStage("submitted", submittedStage);
            }

            List<DataRecord> requestRecordList = drm.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
            if (requestRecordList.size() != 1) {  // error: request ID not found or more than one found
                log.error(String.format("Request %s not found for requestId %s. Returning incomplete information", requestId));
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
                SampleTracker tracker = new SampleTracker(record);

                // Finds all non-failed leaf samples
                AliquotStageTracker parentSample = new AliquotStageTracker(record, user);
                parentSample.setRecord(record);
                parentSample.enrichSample();
                List<AliquotStageTracker> leafSamples = findValidLeafSamples(parentSample, new ArrayList<>(), user);

                // Tracker is failed until it finds a leaf node that is not failed
                tracker.setFailed(Boolean.TRUE);

                // Find all paths from leaf to parent nodes
                List<List<AliquotStageTracker>> paths = new ArrayList<>();
                for (AliquotStageTracker node : leafSamples) {
                    List<AliquotStageTracker> path = new ArrayList<>();
                    node = traverseAndLinkPath(node, path);
                    while (node != null) {
                        // All non-leaf samples are complete (workflow advanced w/ child sample)
                        node.setComplete(Boolean.TRUE);
                        node = traverseAndLinkPath(node, path);
                    }
                    for(AliquotStageTracker n : path){
                        n.assignStageToAmbiguousSamples();
                    }
                    // Leaf nodes are treated differently
                    node = path.get(0);
                    enrichLeafSample(node);

                    // Sample will be failed unless there is at least one path w/ a non-failed leaf sample
                    tracker.setFailed(node.getFailed() && tracker.getFailed());

                    // Get path samples in order from parent to their leaf samples
                    Collections.reverse(path);
                    paths.add(path);
                }
                tracker.setPaths(paths);

                // Calculate status of stages in the sample
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

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("request", request.toApiResponse());
            apiResponse.put("requestId", this.requestId);
            apiResponse.put("serviceId", serviceId);

            return apiResponse;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Appends to path of samples, that should be the current path from a leaf node to its root.
     * Adds a link to the child from node that is traversed to.
     *
     * @param child
     * @param path
     * @return
     */
    private AliquotStageTracker traverseAndLinkPath(AliquotStageTracker child, List<AliquotStageTracker> path) {
        AliquotStageTracker parent = child.getParent();
        if(parent != null){
            // Root is reached when parent is null
            parent.setChild(child);
        }
        path.add(child);

        return parent;
    }
}
