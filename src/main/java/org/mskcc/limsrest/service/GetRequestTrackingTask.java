package org.mskcc.limsrest.service;

import com.mysql.fabric.Server;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.exception.recoverability.serverexception.UnrecoverableServerException;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.BankedSampleModel;
import com.velox.sloan.cmo.recmodels.RequestModel;
import com.velox.sloan.cmo.recmodels.SampleModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.platform.commons.util.StringUtils;
import org.mskcc.domain.sample.CmoSampleInfo;
import org.mskcc.domain.sample.Sample;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;
import org.mskcc.limsrest.service.requesttracker.*;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

import static org.mskcc.limsrest.service.requesttracker.StageTracker.SAMPLE_COUNT;
import static org.mskcc.limsrest.util.StatusTrackerConfig.*;
import static org.mskcc.limsrest.util.Utils.*;

/**
 * Queued task for retrieving tracking status on an input request
 *
 * @author David Streid
 */
public class GetRequestTrackingTask {
    private static Log log = LogFactory.getLog(GetRequestTrackingTask.class);

    private static String[] requestDataLongFields = new String[]{RequestModel.RECEIVED_DATE, "DueDate"};
    private static String[] requestDataStringFields = new String[]{
            RequestModel.LABORATORY_HEAD,
            RequestModel.GROUP_LEADER,
            RequestModel.TATFROM_IN_PROCESSING,
            RequestModel.TATFROM_RECEIVING,
            RequestModel.PROJECT_MANAGER,
            RequestModel.LAB_HEAD_EMAIL,
            RequestModel.INVESTIGATOR,
            RequestModel.PROJECT_NAME,
            RequestModel.REQUEST_NAME

    };
    private ConnectionLIMS conn;
    private String requestId;
    private User user;

    public GetRequestTrackingTask(String requestId, ConnectionLIMS conn) {
        this.requestId = requestId;
        this.conn = conn;
        this.user = conn.getConnection().getUser();
    }

    public Map<String, Object> execute() throws IoError, RemoteException, NotFound, ServerException {
        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager drm = vConn.getDataRecordManager();

        String serviceId = getBankedSampleServiceId(this.requestId, user, drm);
        Request request = new Request(this.requestId);

        if (serviceId != null && !serviceId.equals("")) {
            // Add "submitted" stage if a serviceID exists
            // TODO - Why is this the case (QA: "06302_W")
            // TODO - Place in thread as this can be executed independently
            StageTracker submittedStage = getSubmittedStage(serviceId, user, drm);
            request.addStage(STAGE_SUBMITTED, submittedStage);
        }

        // Validate request record
        final String query = String.format("%s = '%s'", RequestModel.REQUEST_ID, requestId);
        List<DataRecord> requestRecordList = drm.queryDataRecords(RequestModel.DATA_TYPE_NAME, query, user);
        if (requestRecordList.size() != 1) {  // error: request ID not found or more than one found
            log.error(String.format("Request %s not found for requestId %s. Returning incomplete information", requestId, requestId));
            return request.toApiResponse();
        }
        DataRecord requestRecord = requestRecordList.get(0);

        List<ProjectSample> projectSamples = getProjectSamplesFromDataRecord(requestRecord, user);
        request.setSamples(projectSamples);

        Map<String, Object> metaData = getMetaDataFromRecord(requestRecord, this.requestId, serviceId, projectSamples, user);
        request.setMetaData(metaData);

        // Aggregate Project-Level stage information. Stages are added one-by-one as previous stages (E.g. "submitted")
        // may have been added and should not be overwritten
        Map<String, StageTracker> projectStages = getProjectStagesFromSamples(projectSamples);
        for (Map.Entry<String, StageTracker> requestStage : projectStages.entrySet()) {
            request.addStage(requestStage.getKey(), requestStage.getValue());
        }

        Map<String, Object> projectSummary = getProjectSummary(requestRecord, request.getStages(), user);
        request.setSummary(projectSummary);

        return request.toApiResponse();
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
    private Map<String, Object> getProjectSummary(DataRecord requestRecord, Map<String, StageTracker> stages, User user) {
        Map<String, Object> projectStatus = new HashMap<>();

        final Long mostRecentDeliveryDate = getRecordLongValue(requestRecord, RequestModel.RECENT_DELIVERY_DATE, user);
        final Long completedDate = getRecordLongValue(requestRecord, RequestModel.COMPLETED_DATE, user);
        projectStatus.put(RequestModel.RECENT_DELIVERY_DATE, mostRecentDeliveryDate);
        projectStatus.put(RequestModel.COMPLETED_DATE, completedDate);

        boolean isIgoComplete = isIgoComplete(requestRecord, user);
        projectStatus.put("isIgoComplete", isIgoComplete);
        if(isExtractionRequest(requestRecord, user)){
            // Extraction requests are considered delivered if they are IGO-Complete
            projectStatus.put("isDelivered", isIgoComplete);
        } else {
            // Sequencing requests MUST have a delivery date
            projectStatus.put("isDelivered", mostRecentDeliveryDate != null);
        }

        Boolean isStagesComplete = true;
        Integer numFailed = 0;
        Integer numTotal = 0;
        StageTracker stage;
        Integer numComplete = 0;
        String pendingStageName = STAGE_COMPLETE;
        String stageName;
        for (Map.Entry<String, StageTracker> requestStage : stages.entrySet()) {
            stageName = requestStage.getKey();
            stage = requestStage.getValue();
            if (pendingStageName.equals(STAGE_COMPLETE) && !stage.getComplete()) {
                pendingStageName = stageName;
            }
            isStagesComplete = isStagesComplete && stage.getComplete();
            numFailed += stage.getFailedSamplesCount();
            numTotal = stage.getSize() > numTotal ? stage.getSize() : numTotal;
            // Reset numComplete as the # of ending samples in last stage (stages is ordered)
            numComplete = stage.getEndingSamples();
        }
        projectStatus.put("stagesComplete", isStagesComplete);
        projectStatus.put("completed", numComplete);
        projectStatus.put("total", numTotal);
        projectStatus.put("failed", numFailed);
        projectStatus.put("pendingStage", pendingStageName);

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
    private List<ProjectSample> getProjectSamplesFromDataRecord(DataRecord requestRecord, User user) throws IoError, RemoteException, UnrecoverableServerException {
        // Immediate samples of record represent physical samples. LIMS creates children of these in the workflow
        DataRecord[] samples = requestRecord.getChildrenOfType(SampleModel.DATA_TYPE_NAME, user);

        // Create the tree of each ProjectSample aggregating per-sample status/stage information
        List<ProjectSample> projectSamples = new ArrayList<>();
        for (DataRecord sampleRecord : samples) {
            ProjectSampleTree tree = createProjectSampleTree(requestRecord, sampleRecord, user);
            ProjectSample projectSample = tree.evaluateProjectSample();
            projectSamples.add(projectSample);
        }

        return projectSamples;
    }

    /**
     * Populates an input @tree (or subtree) representing a Project Sample using recursive depth-first search, DFS. Will
     * gather the ExemplarSampleStatus of each Sample DataRecord in the workflow to determine the Stages in the workflow
     * <p>
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
     *          - Note: A Failed WorkflowSample is complete - See @markFailedBranch
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
    private ProjectSampleTree createWorkflowTree(WorkflowSample root, ProjectSampleTree tree) {
        tree.addStageToTracked(root);   // Update tree Project Sample stages w/ the input Workflow sample's stage

        // Add any DNA updates
        String rStage = root.getStage();
        if(STAGE_LIBRARY_PREP.equals(rStage) || STAGE_LIBRARY_CAPTURE.equals(rStage)){
            tree.enrichQuantity(root.getRecord(), rStage);
        }

        if (hasFailedQcStage(root)) {
            root.setFailed(Boolean.TRUE);
            tree.markFailedBranch(root);
        }

        // Search each child of the input
        DataRecord[] children = new DataRecord[0];
        try {
            children = root.getRecord().getChildrenOfType(SampleModel.DATA_TYPE_NAME, tree.getUser());
        } catch (IoError | RemoteException | UnrecoverableServerException e) { /* Expected - No more children of the sample */ }

        if (children.length == 0) {
            tree.updateTreeOnLeafStatus(root);
            return tree;
        } else {
            // Sample w/ children is complete
            root.setComplete(Boolean.TRUE);

            // Add all data for the root's children at that level. Allows us to fail only the failed branch
            List<WorkflowSample> workflowChildren = new ArrayList<>();
            for (DataRecord record : children) {
                if (isWorkflowSampleInProject(record, this.requestId, this.user)) {
                    WorkflowSample sample = new WorkflowSample(record, this.conn);

                    // Children are related to the same Qc Records as their parents.
                    sample.addSeqAnalysisQcRecords(root.getSeqAnalysisQcRecords());

                    sample.setParent(root);
                    if (STAGE_AWAITING_PROCESSING.equals(sample.getStage())) {
                        // Update the stage of the sample to the parent stage if it is awaitingProcessing. If there is
                        // not resolved parent, leave as "Awaiting Processing"
                        if (!STAGE_AWAITING_PROCESSING.equals(root.getStage())) {
                            sample.setStage(root.getStage());
                        }
                    }
                    root.addChild(sample);
                    tree.addSample(sample);
                    workflowChildren.add(sample);
                } else {
                    // Child is from a different project - add it as a child Sample Id
                    String childSampleId = getRecordStringValue(record, SampleModel.SAMPLE_ID, this.user);
                    root.setChildSampleId(childSampleId);
                }
            }

            for (WorkflowSample sample : workflowChildren) {
                // Update tree w/ each sample
                log.debug(String.format("Searching child, %d, of data record ID: %d", sample.getRecord().getRecordId(),
                        root.getRecordId()));
                tree = createWorkflowTree(sample, tree);
            }
        }
        return tree;
    }

    /**
     * Returns status of whether the input WorkflowSample has failed seqAnalysisQcRecords
     *
     * @param node
     */
    private boolean hasFailedQcStage(WorkflowSample node) {
        String nodeStage = node.getStage();
        List<DataRecord> seqAnalysisQcRecords = node.getSeqAnalysisQcRecords();

        if (seqAnalysisQcRecords.size() == 0 || !STAGE_SEQUENCING.equals(nodeStage)) {
            // ONLY update projects with @STAGE_SEQUENCING WorkflowSamples that have a WorkflowSample in their path w/ a
            // SeqAnalysisQcRecord child, which means that their sequencing results are going through QC
            return false;
        }

        String qcStatus = getDataQcStatus(seqAnalysisQcRecords, this.user);
        if (QcStatus.FAILED.toString().equals(qcStatus)) {
            return true;
        }
        return false;
    }


    /**
     * Returns whether the sample is part of the project based on whether the sampleId contains the requestId
     *      e.g.
     *          SampleId: "09641_U_76", RequestId: "09641_U" -> TRUE
     *          SampleId: "09641_X_76", RequestId: "09641_U" -> FALSE
     * @param record
     * @param requestId
     * @param user
     * @return
     */
    private boolean isWorkflowSampleInProject(DataRecord record, String requestId, User user) {
        String sampleId = getRecordStringValue(record, SampleModel.SAMPLE_ID, user);
        return sampleId.contains(requestId);
    }

    /**
     * Creates data model of the tree for the DataRecord corresponding to a ProjectSample
     *
     * @param record
     * @param user
     * @return
     */
    private ProjectSampleTree createProjectSampleTree(DataRecord requestRecord, DataRecord record, User user) {
        // Initialize input
        WorkflowSample root = new WorkflowSample(record, this.conn);
        ProjectSampleTree rootTree = new ProjectSampleTree(root, user);
        rootTree.addSample(root);

        // Evaluate overall QcStatus of ProjectSample from all descending SeqAnalysisSampleQC entries b/c the rule is
        // simple - if there is an IGO-Complete SeqAnalysisSampleQC record, the projectSample is IgoComplete
        try {
            List<DataRecord> sampleQcRecords = record.getDescendantsOfType(SeqAnalysisSampleQCModel.DATA_TYPE_NAME, this.user);
            if (sampleQcRecords.size() > 0) {
                String qcStatus = getDataQcStatus(sampleQcRecords, this.user);
                rootTree.setDataQcStatus(qcStatus);
            }
        } catch (RemoteException | IoError | UnrecoverableServerException e) {
            log.error(String.format("Unable to query descending SeqAnalysisSampleQC DataRecords from Sample DataRecord: %d",
                    record.getRecordId()));
        }

        // Evaluate overall status of sample as complete or not
        if(isIgoComplete(requestRecord, user) || rootTree.isQcIgoComplete()){
            rootTree.setIgoComplete(true);
        }

        String investigatorSampleName = getRecordStringValue(record, Sample.USER_SAMPLE_ID, user);
        String sampleName = getRecordStringValue(record, Sample.OTHER_SAMPLE_ID, user);
        rootTree.setSampleName(sampleName);
        rootTree.setInvestigatorSampleId(investigatorSampleName);
        DataRecord[] sampleCmoInfochildren = getChildrenofDataRecord(record, CmoSampleInfo.DATA_TYPE_NAME, user);
        if(sampleCmoInfochildren.length == 1){
            DataRecord sampleCmoInfoChild = sampleCmoInfochildren[0];
            String correctedCmoId = getRecordStringValue(sampleCmoInfoChild, CmoSampleInfo.USER_SAMPLE_ID, user);
            rootTree.setCorrectedInvestigatorSampleId(correctedCmoId);
        } else {
            log.info(String.format("There is not a single %s child for Sample DataRecord: %d",
                    CmoSampleInfo.DATA_TYPE_NAME,
                    record.getRecordId()));
        }
        // Recursively create the workflowTree from the input tree
        ProjectSampleTree workflowTree = createWorkflowTree(root, rootTree);

        return workflowTree;
    }

    /**
     * Aggregate SampleStageTracker from all input @projectSamples into a Project summary of the progress of each
     * Project Sample in the request. This aggregation takes from all ProjectSamples in the project and implements
     * merging logic of the stages
     *
     * @param projectSamples - All samples of a request
     * @return
     */
    public Map<String, StageTracker> getProjectStagesFromSamples(List<ProjectSample> projectSamples) {
        List<StageTracker> sampleStages = projectSamples.stream()
                .flatMap(tracker -> tracker.getStages().stream())
                .collect(Collectors.toList());
        Map<String, StageTracker> requestStagesMap = aggregateStages(sampleStages);
        return requestStagesMap;
    }

    /**
     * Calculates the stage the overall project is at by aggregating the stages of each projectSample in the project.
     * On merge event, update the following -
     *      start/update times
     *      Total
     *      failed Samples
     *      ending Samples
     *
     * @param sampleStages - List of SampleStageTracker instances representing one stage of one sample
     * @return
     */
    public Map<String, StageTracker> aggregateStages(List<StageTracker> sampleStages) {
        Map<String, StageTracker> stageMap = new TreeMap<>(new StageComp());
        if (sampleStages.size() == 0) return stageMap;

        String stageName;
        StageTracker projectStage;    // SampleStageTracker of the project created from aggregated sampleStages
        Boolean isFailedStage;
        for (StageTracker sampleStage : sampleStages) {
            stageName = sampleStage.getStage();
            // Merge Event - Stage added by a previous ProjectSample
            if (stageMap.containsKey(stageName)) {
                projectStage = stageMap.get(stageName);
                projectStage.updateStageTimes(sampleStage);
                projectStage.addStartingSample(SAMPLE_COUNT);
            } else {
                projectStage = new StageTracker(stageName, SAMPLE_COUNT, 0, sampleStage.getStartTime(), sampleStage.getUpdateTime());
                stageMap.put(stageName, projectStage);
            }
            isFailedStage = sampleStage.getFailedSamplesCount() > 0;
            if (sampleStage.getComplete() && !isFailedStage) {
                // Only non-failed, completed stages are considered to have "ended" the stage
                projectStage.addEndingSample(SAMPLE_COUNT);
            }
            // Incremement the number of failed samples in the aggregated
            if (isFailedStage) {
                projectStage.addFailedSample();
            }
        }

        /**
         * A stage is marked complete if,
         *      1) (# ending) + (# failed) = total
         *      2) Previous stage is complete
         */
        Boolean complete = true;
        Integer completedCount;
        Collection<StageTracker> trackers = stageMap.values();
        for (StageTracker tracker : trackers) {
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
    private StageTracker getSubmittedStage(String serviceId, User user, DataRecordManager drm) {
        Map<String, Integer> tracker = new HashMap<>();

        String query = String.format("%s = '%s'", BankedSampleModel.SERVICE_ID, serviceId);
        List<DataRecord> bankedList = new ArrayList<>();
        try {
            bankedList = drm.queryDataRecords(BankedSampleModel.DATA_TYPE_NAME, query, user);
        } catch (NotFound | IoError | RemoteException | ServerException e) {
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
        promoted = tracker.computeIfAbsent(BankedSampleModel.PROMOTED, k -> 0);
        StageTracker requestStage = new StageTracker(STAGE_SUBMITTED, total, promoted, null, null);

        // Submitted stage is complete if at least one sample has been promoted
        requestStage.setComplete(promoted > 0);

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
        } catch (NotFound | IoError | RemoteException | ServerException e) {
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
    private Map<String, Object> getMetaDataFromRecord(DataRecord requestRecord, String requestId, String serviceId, List<ProjectSample> projectSamples, User user) {
        Map<String, Object> metaData = new HashMap<>();
        for (String field : requestDataStringFields) {
            metaData.put(field, getRecordStringValue(requestRecord, field, user));
        }
        for (String field : requestDataLongFields) {
            metaData.put(field, getRecordLongValue(requestRecord, field, user));
        }

        metaData.put("requestId", requestId);
        metaData.put("serviceId", serviceId);

        // Certain fields can only be collected after the ProjectSample has been created
        Set<String> sourceRequests = projectSamples.stream()
                .map(sample -> sample.getRoot().getSourceRequestId())
                .filter(sampleId -> !StringUtils.isBlank(sampleId))
                .collect(Collectors.toSet());
        metaData.put("sourceRequests", sourceRequests.toArray());

        // Certain fields can only be collected after the ProjectSample has been created
        Set<String> childRequests = projectSamples.stream()
                .map(sample -> sample.getRoot().getChildRequestIds())
                .flatMap(List::stream)
                .filter(sampleId -> !StringUtils.isBlank(sampleId))
                .collect(Collectors.toSet());
        metaData.put("childRequests", childRequests.toArray());

        return metaData;
    }
}
