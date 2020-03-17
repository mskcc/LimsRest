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
import java.util.stream.Stream;

import static org.mskcc.limsrest.service.requesttracker.StatusTrackerConfig.*;
import static org.mskcc.limsrest.util.DataRecordAccess.*;

public class GetRequestTrackingTask {
    private static Log log = LogFactory.getLog(GetRequestTrackingTask.class);
    private static Integer SAMPLE_COUNT = 1;
    private static String[] requestDataLongFields = new String[]{"RecentDeliveryDate", "ReceivedDate"};
    private static String[] requestDataStringFields = new String[]{
            "LaboratoryHead",
            "GroupLeader",
            "TATFromInProcessing",
            "TATFromReceiving",
            "ProjectManager",
            "LabHeadEmail",
            "Investigator"
    };
    private ConnectionLIMS conn;
    private String requestId;

    public GetRequestTrackingTask(String requestId, ConnectionLIMS conn) {
        this.requestId = requestId;
        this.conn = conn;
    }

    /**
     * Populates the metaData for the request using the Record from the Request DataType
     *
     * @param requestRecord - DataRecord from the Request DataType
     * @param user
     * @return
     */
    private Map<String, Object> populateMetaData(DataRecord requestRecord, User user){
        Map<String, Object> requestMetaData = new HashMap<>();
        for (String field : requestDataStringFields) {
            requestMetaData.put(field, getRecordStringValue(requestRecord, field, user));
        }
        for (String field : requestDataLongFields) {
            requestMetaData.put(field, getRecordLongValue(requestRecord, field, user));
        }
        return requestMetaData;
    }

    /**
     * Projects are considered IGO-Complete if they have a delivery date
     *
     * @param requestRecord
     * @param user
     * @return
     */
    private Boolean isIgoComplete(DataRecord requestRecord, User user) {
        return getRecordLongValue(requestRecord, "RecentDeliveryDate", user) != null;
    }

    public Map<String, Object> execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager drm = vConn.getDataRecordManager();

            String serviceId = getBankedSampleServiceId(this.requestId, user, drm);
            Request request = new Request(requestId, serviceId);

            if (serviceId != null && !serviceId.equals("")) {
                // Add "submitted" stage if a serviceID exists
                // TODO - Why is this the case (QA: "06302_W")
                // TODO - Place in thread as this can be executed independently
                SampleStageTracker submittedStage = getSubmittedStage(serviceId, user, drm);
                request.addStage("submitted", submittedStage);
            }

            List<DataRecord> requestRecordList = drm.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
            if (requestRecordList.size() != 1) {  // error: request ID not found or more than one found
                log.error(String.format("Request %s not found for requestId %s. Returning incomplete information", requestId));
                return request.toApiResponse();
            }

            DataRecord requestRecord = requestRecordList.get(0);
            request.setMetaData(populateMetaData(requestRecord, user));
            request.setIgoComplete(isIgoComplete(requestRecord, user));

            // Immediate samples of the request. These samples represent the overall progress of each project sample
            DataRecord[] samples = requestRecord.getChildrenOfType("Sample", user);

            // Find paths & calculate stages for each sample in the request
            for (DataRecord record : samples) {
                SampleTracker tracker = new SampleTracker(record);

                AliquotStageTracker parentSample = new AliquotStageTracker(record, user);
                parentSample.setRecord(record);
                parentSample.enrichSample();

                SampleTreeTracker tree = createSampleTree(record, user);

                tracker.addStage(tree.getStageMap());
                Boolean isFailed = Boolean.TRUE;    // One non-failed sample will set this to True
                Boolean isComplete = Boolean.TRUE;  // All sub-samples need to be complete or the sample to be complete
                for(AliquotStageTracker sample : tree.getSampleMap().values()){
                    isFailed = isFailed && sample.getFailed();
                    isComplete = isComplete && sample.getComplete();
                }
                tracker.setFailed(isFailed);
                tracker.setComplete(isComplete);
                tracker.setRoot(tree.getRoot());

                request.addSampleTracker(tracker);
            }

            // Aggregate sample stage information into the request level
            List<SampleTracker> sampleTrackers = request.getSamples();
            List<SampleStageTracker> requestStages = sampleTrackers.stream()
                    .flatMap(tracker -> tracker.getStages().values().stream())
                    .collect(Collectors.toList());
            Map<String, SampleStageTracker> requestStagesMap = aggregateStages(requestStages);

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
     * Populates input @tree w/ based on the input @root sample & its children.
     * Assumptions -
     *      Sample w/ child samples is completed - moving on in the workflow creates a child sample
     *      Sample w/o child samples is incomplete, unless it has a status that indicates a completed workflow
     *      All nodes in the path to a failed leaf sample have failed
     *      All nodes in a path to a Non-failed leaf sample have not failed
     *      There only needs to be one non-failed leaf sample or the whole tree to have not failed
     *      Samples that are awaiting processing or have an ambiguous status will take their stage from their parent
     *      Samples are incomplete/not-failed until shown to be otherwise
     *
     * @param root - Enriched model of a Sample record's data
     * @param tree - Data Model for tracking Sample Stages, samples, and the root node
     * @return
     */
    private SampleTreeTracker searchSampleTree(AliquotStageTracker root, SampleTreeTracker tree){
        User user = tree.getUser();
        tree.updateStage(root);
        DataRecord[] children = new DataRecord[0];
        try {
            children = root.getRecord().getChildrenOfType("Sample", user);
        } catch (IoError | RemoteException e) { /* Expected - No more children of the sample */ }

        if (children.length == 0) {
            tree.updateLeafStageCompletionStatus(root);

            if(root.getFailed()){
                // Iterate up from sample and set all samples to false
                AliquotStageTracker parent = root.getParent();
                while(parent != null){
                    parent.setFailed(Boolean.TRUE);
                    parent = parent.getParent();
                }
            }
            return tree;
        } else {
            // Sample w/ children is complete
            root.setComplete(Boolean.TRUE);
            List<AliquotStageTracker> samples = new ArrayList<>();

            for(DataRecord record : children){
                AliquotStageTracker sample = new AliquotStageTracker(record, user);
                sample.setParent(root);
                sample.setComplete(Boolean.FALSE);      // All Samples are incomplete until child is found
                sample.setFailed(Boolean.FALSE);
                sample.enrichSample();
                if(STAGE_UNKNOWN.equals(sample.getStage()) || STAGE_AWAITING_PROCESSING.equals(sample.getStage())){
                    // Update the stage of the sample to the parent stage if it is unknown
                    if(!STAGE_UNKNOWN.equals(root.getStage())){
                        sample.setStage(root.getStage());
                    }
                }

                root.addChild(sample);
                tree.addSample(sample);

                // Update tree w/ each sample
                log.info(String.format("Searching children of root record: %d", root.getRecordId()));

                tree = searchSampleTree(sample, tree);
            }
        }
        return tree;
    }

    /**
     * Creates data model of the tree of samples descending from the root parent sample
     *
     * @param record
     * @param user
     * @return
     */
    private SampleTreeTracker createSampleTree(DataRecord record, User user) {
        AliquotStageTracker root = new AliquotStageTracker(record, user);
        root.enrichSample();
        SampleTreeTracker inputTree = new SampleTreeTracker(root, user);
        inputTree.addSample(root);
        SampleTreeTracker populatedTree = searchSampleTree(root, inputTree);

        // TODO - Needs to account for partially complete stages
        List<SampleStageTracker> stages = new ArrayList<>(populatedTree.getStageMap().values());
        SampleStageTracker stage;
        for(int i = 0; i<stages.size()-1; i++){
            stage = stages.get(i);
            stage.addEndingSample(1);
            stage.setComplete(Boolean.TRUE);
        }
        // TODO - Clarity: Last stage will have been populated in "searchSampleTree"

        return populatedTree;
    }

    /**
     * Calculates the stage the overall sample is at based on the least advanced path
     *
     * @param path - List of Aliquot/Sample trackers
     * @return
     */
    public Map<String, SampleStageTracker> aggregateStages(List<? extends StageTracker> path) {
        Map<String, SampleStageTracker> stageMap = new TreeMap<>(new StatusTrackerConfig.StageComp());
        if (path.size() == 0) return stageMap;

        String stageName;
        SampleStageTracker stage;
        for (StageTracker stageTracker : path) {
            stageName = stageTracker.getStage();
            if (stageMap.containsKey(stageName)) {
                stage = stageMap.get(stageName);
                stage.updateStageTimes(stageTracker);
                stage.addStartingSample(SAMPLE_COUNT);
                if (stageTracker.getComplete()) {
                    stage.addEndingSample(SAMPLE_COUNT);
                }
            } else {
                Integer endingCount = stageTracker.getComplete() ? SAMPLE_COUNT : 0;
                stage = new SampleStageTracker(stageName, SAMPLE_COUNT, endingCount, stageTracker.getStartTime(), stageTracker.getUpdateTime());
                stageMap.put(stageName, stage);
            }
        }

        // Calculate completion status of stage based on whether endingSamples equals total size
        stageMap.values().forEach(
                tracker -> {
                    tracker.setComplete(tracker.getEndingSamples() == tracker.getSize());
                }
        );

        return stageMap;
    }

    /**
     * Retrieves the Submitted Stage of the sample
     *
     * @param serviceId
     * @param user
     * @param drm
     * @return
     */
    private SampleStageTracker getSubmittedStage(String serviceId, User user, DataRecordManager drm) {
        Map<String, Integer> tracker = new HashMap<>();

        String query = String.format("ServiceId = '%s'", serviceId);
        List<DataRecord> bankedList = new ArrayList<>();
        try {
            bankedList = drm.queryDataRecords("BankedSample", query, user);
        } catch (NotFound | IoError | RemoteException e) {
            log.info(String.format("Could not find BankedSample record for %s", serviceId));
            return null;
        }

        Boolean wasPromoted;
        Integer total;
        Integer promoted;
        for (DataRecord record : bankedList) {
            // Increment total
            total = tracker.computeIfAbsent("Total", k -> 0);
            tracker.put("Total", total + 1);

            // Increment promoted if sample was promoted
            promoted = tracker.computeIfAbsent("Promoted", k -> 0);
            wasPromoted = getRecordBooleanValue(record, "Promoted", user);
            if (wasPromoted) {
                tracker.put("Promoted", promoted + 1);
            }
        }

        total = tracker.get("Total");
        promoted = tracker.get("Promoted");

        SampleStageTracker requestStage = new SampleStageTracker("submitted", total, promoted, null, null);
        Boolean submittedComplete = total == promoted;
        if (submittedComplete) {
            requestStage.setComplete(Boolean.TRUE);
        } else {
            requestStage.setComplete(Boolean.FALSE);
        }

        return requestStage;
    }

    /**
     * Returns the serviceId for the input requestId
     *
     * @param requestId
     * @param user
     * @param drm
     * @return
     */
    public String getBankedSampleServiceId(String requestId, User user, DataRecordManager drm) {
        String query = String.format("RequestId = '%s'", requestId);
        List<DataRecord> bankedList = new ArrayList<>();
        try {
            bankedList = drm.queryDataRecords("BankedSample", query, user);
        } catch (NotFound | IoError | RemoteException e) {
            log.info(String.format("Could not find BankedSample records w/ RequestId: %s", requestId));
            return null;
        }

        Set<String> serviceIds = new HashSet<>();
        String serviceId;
        for (DataRecord record : bankedList) {
            serviceId = getRecordStringValue(record, "ServiceId", user);
            serviceIds.add(serviceId);
        }

        if (serviceIds.size() != 1) {
            String error = String.format("Failed to retrieve serviceId for RequestId: %s. ServiceIds Returned: %s",
                    requestId, serviceIds.toString());
            log.error(error);
            return "";
        }

        return new ArrayList<>(serviceIds).get(0);
    }
}
