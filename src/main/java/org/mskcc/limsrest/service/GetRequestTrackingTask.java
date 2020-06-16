package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.requesttracker.*;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

import static org.mskcc.limsrest.util.Utils.*;
import static org.mskcc.limsrest.util.StatusTrackerConfig.*;

public class GetRequestTrackingTask {
    private static Log log = LogFactory.getLog(GetRequestTrackingTask.class);
    private static Integer SAMPLE_COUNT = 1;

    private static String[] requestDataLongFields = new String[]{RequestModel.RECEIVED_DATE};
    private static String[] requestDataStringFields = new String[]{
            RequestModel.LABORATORY_HEAD,
            RequestModel.GROUP_LEADER,
            RequestModel.TATFROM_IN_PROCESSING,
            RequestModel.TATFROM_RECEIVING,
            RequestModel.PROJECT_MANAGER,
            RequestModel.LAB_HEAD_EMAIL,
            RequestModel.INVESTIGATOR
    };
    private ConnectionLIMS conn;
    private String requestId;

    public GetRequestTrackingTask(String requestId, ConnectionLIMS conn) {
        this.requestId = requestId;
        this.conn = conn;
    }

    public Map<String, Object> execute() throws IoError, RemoteException, NotFound {
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
            request.addStage(STAGE_SUBMITTED, submittedStage);
        }

        // Validate request record

        final String query = String.format("%s = '%s'", RequestModel.REQUEST_ID, requestId);
        List<DataRecord> requestRecordList = drm.queryDataRecords(RequestModel.DATA_TYPE_NAME, query, user);
        if (requestRecordList.size() != 1) {  // error: request ID not found or more than one found
            log.error(String.format("Request %s not found for requestId %s. Returning incomplete information", requestId));
            return request.toApiResponse();
        }
        DataRecord requestRecord = requestRecordList.get(0);

        Map<String, Object> metaData = getMetaDataFromRecord(requestRecord, user);
        request.setMetaData(metaData);

        List<ProjectSample> projectSamples = getProjectSamplesFromDataRecord(requestRecord, user);
        request.setSamples(projectSamples);

        Map<String, SampleStageTracker> projectStages = getProjectStagesFromSamples(projectSamples);
        for (Map.Entry<String, SampleStageTracker> requestStage : projectStages.entrySet()) {
            request.addStage(requestStage.getKey(), requestStage.getValue());
        }

        Map<String, Object> projectSummary = getProjectSummary(requestRecord, request.getStages(), user);
        request.setSummary(projectSummary);

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("request", request.toApiResponse());
        apiResponse.put("requestId", this.requestId);
        apiResponse.put("serviceId", serviceId);

        return apiResponse;
    }

    /**
     * Returns a projectSummary object
     *      e.g.
     *          "summary": {
     *              "total": 10,
     *              "RecentDeliveryDate": null,
     *              "stagesComplete": false,
     *              "isIgoComplete": false,
     *              "completed": 9,
     *              "failed": 0
     *          }
     *
     * @param requestRecord
     * @param stages
     * @param user
     * @return
     */
    private Map<String, Object> getProjectSummary(DataRecord requestRecord, Map<String, SampleStageTracker> stages, User user){
        Map<String, Object> projectStatus = new HashMap<>();

        // IGO Completion is confirmed by delivery, which sets the "RecentDeliveryDate" field
        final Long mostRecentDeliveryDate = getRecordLongValue(requestRecord, RequestModel.RECENT_DELIVERY_DATE, user);
        if(mostRecentDeliveryDate != null){
            projectStatus.put("isIgoComplete", true);
        } else {
            projectStatus.put("isIgoComplete", false);
        }
        projectStatus.put(RequestModel.RECENT_DELIVERY_DATE, mostRecentDeliveryDate);

        Boolean isStagesComplete = true;
        Integer numFailed = 0;
        Integer numComplete = 0;
        Integer numTotal = 0;
        SampleStageTracker stage;
        for (Map.Entry<String, SampleStageTracker> requestStage : stages.entrySet()) {
            stage = requestStage.getValue();
            isStagesComplete = isStagesComplete && stage.getComplete();
            numFailed += stage.getFailedSamplesCount();
            numTotal = stage.getSize() > numTotal ? stage.getSize() : numTotal;
            numComplete = stage.getEndingSamples();

        }
        projectStatus.put("stagesComplete", isStagesComplete);
        projectStatus.put("completed", numComplete);
        projectStatus.put("total", numTotal);
        projectStatus.put("failed", numFailed);

        return projectStatus;
    }

    /**
     * Traverse the tree of each "Sample" DataType child of the input @requestRecord. This tree is converted into
     * a ProjectSample data model that represents the tree
     *
     * @param requestRecord - DataRecord of the request being tracked
     * @param user
     * @return
     * @throws IoError
     * @throws RemoteException
     */
    private List<ProjectSample> getProjectSamplesFromDataRecord(DataRecord requestRecord, User user) throws IoError, RemoteException  {
        // Immediate samples of record represent physical samples. LIMS creates children of these in the workflow
        DataRecord[] samples = requestRecord.getChildrenOfType(SampleModel.DATA_TYPE_NAME, user);

        // Create the tree of each ProjectSample aggregating per-sample status/stage information
        List<ProjectSample> projectSamples = new ArrayList<>();
        for (DataRecord record : samples) {
            ProjectSampleTree tree = createProjectSampleTree(record, user);
            ProjectSample projectSample = tree.convertToProjectSample();
            projectSamples.add(projectSample);
        }

        return projectSamples;
    }

    /**
     * Populates an input @tree (or subtree) representing a Project Sample using a recursive depth-first search. Will
     * gather the ExemplarSampleStatus of each Sample DataRecord in the workflow to determine the Stages in the workflow
     *
     * Implementation Notes:
     *  SAMPLES (WorkflowSample)
     *      A sample can be incomplete, complete, & failed-complete. These are determined by a failed and complete flag.
     *      WorkflowSamples default to incomplete and can only be changed to,
     *          I)  FAILED      on  INITIALIZATION, if they have a failed ExemplarSampleStatus
     *          II) COMPLETE    on  TREE TRAVERSAL, if they have a DataRecord Child in the "Sample" DataType
     *      STATUSES (More Info):
     *      I)  INCOMPLETE: Default                                                 sample.complete = false (on init)
     *      II) FAILED: Has an ExemplarSampleStatus determined to be failed         sample.failed   = true
     *          - All parent WorkflowSamples from a failed leaf are marked failed until a parent w/ non-failed children
     *          - All WorkflowSamples in a path to a Non-failed leaf sample should not be failed
     *          - If the root WorkflowSample has not failed, then the ProjectSample has not failed, i.e. there only
     *            needs to be one non-failed leaf sample in the tree for the whole tree to have not failed
     *          - Note: A Failed WorkflowSample is complete (different from Stage)
     *      III) COMPLETE: Sample has a Sample-DataType DataRecord  child           sample.complete = true
     *          - WorkflowSample w/ child is completed b/c moving on in the workflow creates a child sample
     *          - Non-failed WorkflowSample w/o sample children is incomplete
     *
     *  STAGES (SampleStageTracker)
     *      Each Sample belongs to a Stage, i.e. a SampleStageTracker instance. There are only two possible stage
     *      statuses - COMPLETE/INCOMPLETE. The stage statuses for a ProjectSample determine the overall state of that
     *      sample (Pending, Complete, Failed)
     *      Each WorkflowSample will add/update a SampleStageTracker instance in the tree. The stage is determined by
     *      the ExemplarSampleStatus of the Sample DataRecord EXCEPT FOR,
     *          - If a sample is "Awaiting Processing" or has an ambiguous status. In this case the WorkflowSample's
     *          stage will be that of their parent
     *          - If a Sample DataRecord has a "SeqAnalysisSampleQC" child, it belongs to the Data-QC stage
     *      The Stage of a Sample is initialized to COMPLETE and can only be changed to INCOMPLETE by,
     *          I) A LEAF-SAMPLE        WHEN    the Sample is non-failed and has an incomplete ExemplarSampleStatus
     *          determined by StatusTrackerConfig::isCompletedStatus
     *          II) A NON-LEAF SAMPLE   WHEN    the Sample is determined to be in the Data-QC and determined to have
     *          not passed Data QC by ProjectSampleTree::isPassedDataQc
     *
     * @param root - Workflow sample root whose subtree will be evaluated and update the @tree
     * @param tree - Tree describing the workflow, which is represented in LIMS as a tree descending from the @root
     * @return
     */
    private ProjectSampleTree  createWorkflowTree(WorkflowSample root, ProjectSampleTree tree){
        // TODO - better way to identify which roots could have a Sequencing QC table?
        DataRecord sampleQcRecord = getChildSeqAnalysisSampleQcRecord(root.getRecord(), tree.getUser());
        if(!tree.isPassedDataQc() && sampleQcRecord != null){
            // If the input node has a SeqQCStatus record, it is a part of the DataQC stage
            String sequencingStatus = getRecordStringValue(sampleQcRecord, SeqAnalysisSampleQCModel.SEQ_QCSTATUS, tree.getUser());
            tree.setDataQcStatus(sequencingStatus);
            // Stage needs to be reset
            root.setStage(STAGE_DATA_QC);
        }

        tree.addStageToTracked(root);   // Update the overall Project Sample stages w/ the input Workflow sample's stage

        // Search each child of the input
        DataRecord[] children = new DataRecord[0];
        try {
            children = root.getRecord().getChildrenOfType(SampleModel.DATA_TYPE_NAME, tree.getUser());
        } catch (IoError | RemoteException e) { /* Expected - No more children of the sample */ }

        if (children.length == 0) {
            tree.updateTreeOnLeafStatus(root);
            return tree;
        } else {
            // Sample w/ children is complete
            root.setComplete(Boolean.TRUE);

            // Add all data for the root's children at that level. Allows us to fail only the failed branch
            List<WorkflowSample> workflowChildren = new ArrayList<>();
            for(DataRecord record : children){
                WorkflowSample sample = new WorkflowSample(record, this.conn);
                sample.setParent(root);
                if(STAGE_AWAITING_PROCESSING.equals(sample.getStage())){
                    // Update the stage of the sample to the parent stage if it is unknown
                    if(!STAGE_AWAITING_PROCESSING.equals(root.getStage())){
                        sample.setStage(root.getStage());
                    }
                }
                root.addChild(sample);
                tree.addSample(sample);
                workflowChildren.add(sample);
            }

            for(WorkflowSample sample : workflowChildren){
                // Update tree w/ each sample
                log.debug(String.format("Searching children of data record ID: %d", root.getRecordId()));
                tree = createWorkflowTree(sample, tree);
            }
        }
        return tree;
    }

    /**
     * Creates data model of the tree for the DataRecord corresponding to a ProjectSample
     *
     * @param record
     * @param user
     * @return
     */
    private ProjectSampleTree createProjectSampleTree(DataRecord record, User user) {
        // Initialize input
        WorkflowSample root = new WorkflowSample(record, this.conn);
        ProjectSampleTree rootTree = new ProjectSampleTree(root, user);
        rootTree.addSample(root);

        // Recursively create the workflowTree from the input tree
        ProjectSampleTree workflowTree = createWorkflowTree(root, rootTree);

        return workflowTree;
    }

    /**
     * Aggregate SampleStageTracker from all input @projectSamples. The Project Stages summarize the progress of each
     * Project Sample in the request
     *
     * @param projectSamples - All samples of a request
     * @return
     */
    public Map<String, SampleStageTracker> getProjectStagesFromSamples(List<ProjectSample> projectSamples){
        List<SampleStageTracker> sampleStages = projectSamples.stream()
                .flatMap(tracker -> tracker.getStages().stream())
                .collect(Collectors.toList());
        Map<String, SampleStageTracker> requestStagesMap = aggregateStages(sampleStages);
        return requestStagesMap;
    }

    /**
     * Calculates the stage the overall sample is at based on the least advanced path
     *
     * @param sampleStages - List of SampleStageTracker instances representing one stage of one sample
     * @return
     */
    public Map<String, SampleStageTracker> aggregateStages(List<SampleStageTracker> sampleStages) {
        Map<String, SampleStageTracker> stageMap = new TreeMap<>(new StageComp());
        if (sampleStages.size() == 0) return stageMap;

        String stageName;
        SampleStageTracker projectStage;    // SampleStageTracker of the project created from aggregated sampleStages
        Boolean isFailedStage;
        for (SampleStageTracker sampleStage : sampleStages) {
            stageName = sampleStage.getStage();
            if (stageMap.containsKey(stageName)) {
                projectStage = stageMap.get(stageName);
                projectStage.updateStageTimes(sampleStage);
                projectStage.addStartingSample(SAMPLE_COUNT);
                isFailedStage = sampleStage.getFailedSamplesCount() > 0;
                if (sampleStage.getComplete() && !isFailedStage) {
                    // Only non-failed, completed stages are considered to have "ended" the stage
                    projectStage.addEndingSample(SAMPLE_COUNT);
                }
                // Incremement the number of failed samples in the aggregated
                if (isFailedStage){
                    projectStage.addFailedSample();
                }
            } else {
                Integer endingCount = sampleStage.getComplete() ? SAMPLE_COUNT : 0;
                projectStage = new SampleStageTracker(stageName, SAMPLE_COUNT, endingCount, sampleStage.getStartTime(), sampleStage.getUpdateTime());
                stageMap.put(stageName, projectStage);
            }
        }

        /**
         * A stage is marked complete if,
         *      1) (# ending) + (# failed) = total
         *      2) Previous stage is complete
         */
        Boolean complete = true;
        Integer completedCount;
        Collection<SampleStageTracker> trackers = stageMap.values();
        for(SampleStageTracker tracker : trackers){
            completedCount = tracker.getEndingSamples() + tracker.getFailedSamplesCount();
            complete = complete && completedCount.equals(tracker.getSize());
            tracker.setComplete(complete);
        }

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

        String query = String.format("%s = '%s'", BankedSampleModel.SERVICE_ID, serviceId);
        List<DataRecord> bankedList = new ArrayList<>();
        try {
            bankedList = drm.queryDataRecords(BankedSampleModel.DATA_TYPE_NAME, query, user);
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
            promoted = tracker.computeIfAbsent(BankedSampleModel.PROMOTED, k -> 0);
            wasPromoted = getRecordBooleanValue(record, BankedSampleModel.PROMOTED, user);
            if (wasPromoted) {
                tracker.put(BankedSampleModel.PROMOTED, promoted + 1);
            }
        }

        total = tracker.get("Total");
        promoted = tracker.get(BankedSampleModel.PROMOTED);

        // TODO - constants
        SampleStageTracker requestStage = new SampleStageTracker(STAGE_SUBMITTED, total, promoted, null, null);
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
            bankedList = drm.queryDataRecords(BankedSampleModel.DATA_TYPE_NAME, query, user);
        } catch (NotFound | IoError | RemoteException e) {
            log.info(String.format("Could not find BankedSample records w/ RequestId: %s", requestId));
            return null;
        }

        Set<String> serviceIds = new HashSet<>();
        String serviceId;
        for (DataRecord record : bankedList) {
            serviceId = getRecordStringValue(record, BankedSampleModel.SERVICE_ID, user);
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

    /**
     * Populates the metaData for the request using the Record from the Request DataType
     *
     * @param requestRecord - DataRecord from the Request DataType
     * @param user
     * @return
     */
    private Map<String, Object> getMetaDataFromRecord(DataRecord requestRecord, User user){
        Map<String, Object> requestMetaData = new HashMap<>();
        for (String field : requestDataStringFields) {
            requestMetaData.put(field, getRecordStringValue(requestRecord, field, user));
        }
        for (String field : requestDataLongFields) {
            requestMetaData.put(field, getRecordLongValue(requestRecord, field, user));
        }

        // IGO Completion is confirmed by delivery, which sets the "RecentDeliveryDate" field
        final Long mostRecentDeliveryDate = getRecordLongValue(requestRecord, RequestModel.RECENT_DELIVERY_DATE, user);
        if(mostRecentDeliveryDate != null){
            requestMetaData.put("isIgoComplete", true);
        }
        requestMetaData.put(RequestModel.RECENT_DELIVERY_DATE, mostRecentDeliveryDate);

        return requestMetaData;
    }
}
