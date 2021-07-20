package org.mskcc.limsrest.util;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.api.workflow.Workflow;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.RequestModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

import static org.mskcc.limsrest.util.Utils.*;

/**
 * Logic for determining and ordering stages from LIMS sample statuses and workflows
 *
 * @author David Streid
 */
public class StatusTrackerConfig {
    // STAGES
    public static final String STAGE_SUBMITTED = "Submitted";
    public static final String STAGE_EXTRACTION = "Nucleic Acid Extraction";
    public static final String STAGE_LIBRARY_PREP = "Library Preparation";
    public static final String STAGE_LIBRARY_CAPTURE = "Library Capture";
    public static final String STAGE_SAMPLE_QC = "Sample QC";
    public static final String STAGE_SEQUENCING = "Sequencing";
    public static final String STAGE_SEQUENCING_ANALYSIS = "Illumina Sequencing Analysis";
    public static final String STAGE_IGO_COMPLETE = "IGO Complete";
    public static final String STAGE_LIBRARY_QC = "Library QC";
    public static final String STAGE_PCR = "Digital PCR";
    public static final String STAGE_ADDING_CMO_INFORMATION = "Adding CMO Information";
    public static final String STAGE_PENDING_USER_DECISION = "Pending User Decision";
    public static final String STAGE_COVID_19_ASSAY = "COVID-19 Assay";
    public static final String STAGE_BATCH_PLANNING = "Batch Planning";
    public static final String STAGE_SAMPLE_ALIQUOTS = "Create Sample Aliquots";
    public static final String STAGE_NANOSTRING = "Nano string";
    public static final String STAGE_STR_PCR = "STR PCR";
    public static final String STAGE_STR_ANALYSIS = "STR Analysis";
    public static final String STAGE_SNP_FINGERPRINTING = "SNP Fingerprinting";
    public static final String STAGE_SAMPLE_RECEIPT = "Sample Receipt";
    public static final String STAGE_SAMPLE_REPLACEMENT_COMBINATION = "Sample Replacement/Combination";
    public static final String STAGE_TRANSFER_TUBE_SAMPLES = "Transfer Tube Samples to Plates";
    public static final String STAGE_PATHOLOGY = "Pathology";
    public static final String STAGE_Digital_PCR = "Digital PCR";
    public static final String STAGE_RETURNED_TO_USER = "Returned to User";
    public static final String STAGE_AWAITING_PROCESSING = "Awaiting Processing";   // Use for undetermined, e.g. manual status assignment/new workflow
    public static final String STAGE_COMPLETE = "Completed";                        // Used to indicate no pending stage
    // TODO - This should be added, but in a way that it disappears once it is not longer processing
    // TODO - PROCESSING STAGES: "Ready for Processing", "Awaiting Processing", "In Processing", & "Processing Completed"

    /**
     * Add the order of valid stages here and then the ordering map will be statically initialzed
     */
    public static final String[] STAGE_ORDER = new String[]{
            // TODO - need to confirm w/ Anna/Ajay
            STAGE_SUBMITTED,
            STAGE_AWAITING_PROCESSING,
            STAGE_EXTRACTION,
            STAGE_LIBRARY_PREP,
            STAGE_PENDING_USER_DECISION,
            STAGE_LIBRARY_CAPTURE,
            STAGE_LIBRARY_QC,
            STAGE_SAMPLE_QC,
            STAGE_PCR,
            STAGE_Digital_PCR,
            STAGE_ADDING_CMO_INFORMATION,
            STAGE_COVID_19_ASSAY,
            STAGE_BATCH_PLANNING,
            STAGE_SAMPLE_ALIQUOTS,
            STAGE_NANOSTRING,
            STAGE_STR_PCR,
            STAGE_STR_ANALYSIS,
            STAGE_SNP_FINGERPRINTING,
            STAGE_SAMPLE_RECEIPT,
            STAGE_SAMPLE_REPLACEMENT_COMBINATION,
            STAGE_TRANSFER_TUBE_SAMPLES,
            STAGE_PATHOLOGY,
            STAGE_SEQUENCING,
            STAGE_IGO_COMPLETE,
            STAGE_RETURNED_TO_USER
    };

    /**
     * Returns the position of the stage. Returns out-of-bounds index if not present
     *
     * @param status
     * @return
     */
    public static int getStageOrder(String status) {
        for (int i = 0; i < STAGE_ORDER.length; i++) {
            if (status.equals(STAGE_ORDER[i])) return i;
        }
        return STAGE_ORDER.length;
    }

    /**
     * Comparator used to sort statuses based on their order
     */
    public static class StageComp implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            int p1 = getStageOrder(s1);
            int p2 = getStageOrder(s2);

            return p1 - p2;
        }
    }

    /**
     * Returns whether a project is considered IGO-complete.
     * Criteria:
     *      Extraction Requests - 1) Status: "Completed", 2) Non-null Completed Date
     *      Other - Non-null Recent Delivery Date
     *
     * NOTE - This should be in sync w/ @ToggleSampleQcStatus::setSeqAnalysisSampleQcStatus. If changing this, uncomment
     * @getIgoRequestsTask_matchesIsIgoCompleteUtil_* tests in GetIgoRequestsTaskTest
     * @param record
     * @param user
     * @return
     */
    public static boolean isIgoComplete(DataRecord record, User user) {
        Long mostRecentDeliveryDate = getRecordLongValue(record, RequestModel.RECENT_DELIVERY_DATE, user);
        if (mostRecentDeliveryDate != null) {
            return true;
        }

        // Extraction requests ONLY are considered complete if they have a completion date
        Long completedDate = getRecordLongValue(record, RequestModel.COMPLETED_DATE, user);
        return (completedDate != null) && isExtractionRequest(record, user);
    }

    /**
     * Returns whether input DataRecord represents an Extraction request
     *
     * @param record, Request DataRecord
     * @param user
     * @return
     */
    public static boolean isExtractionRequest(DataRecord record, User user){
        String requestType = getRecordStringValue(record, RequestModel.REQUEST_NAME, user);
        return requestType.toLowerCase().contains("extraction");
    }

    /**
     * This should match ToggleSampleQcStatus > setSeqAnalysisSampleQcStatus
     *
     * @param record
     * @param user
     * @return
     */
    public static boolean isQcStatusIgoComplete(DataRecord record, User user) {
        Boolean passedQc = getRecordBooleanValue(record, SeqAnalysisSampleQCModel.PASSED_QC, user);
        if (!passedQc) {
            return false;
        }
        String seqQcStatus = getRecordStringValue(record, SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user);
        if (!QcStatus.PASSED.getText().equals(seqQcStatus)) {
            return false;
        }
        return true;
        /* TODO - True for samples after 2/20/20. Add after Feb 2021
        Long dateIgoComplete = getRecordLongValue(record, "DateIgoComplete", user);
        return dateIgoComplete != null;
         */
    }

    /**
     * Returns the DataQc status determined from a list of SeqAnalysisSampleQC DataRecords (these should all come from
     * the sample projectSample)
     *
     * @param seqAnalysisQcRecords
     * @return
     */
    public static String getDataQcStatus(List<DataRecord> seqAnalysisQcRecords, User user) {
        List<DataRecord> igoCompleteRecords = seqAnalysisQcRecords.stream().filter(record ->
                isQcStatusIgoComplete(record, user)
        ).collect(Collectors.toList());

        if (igoCompleteRecords.size() > 0) {
            // Only one record needs to be IGO-Complete
            return QcStatus.IGO_COMPLETE.toString();
        }

        List<String> seqQcStatuses = seqAnalysisQcRecords.stream().map(record ->
                getRecordStringValue(record, SeqAnalysisSampleQCModel.SEQ_QCSTATUS, user)
        ).collect(Collectors.toList());

        Integer numFailed = seqQcStatuses.stream()
                .filter(status -> QcStatus.FAILED.toString().equalsIgnoreCase(status))
                .collect(Collectors.toList())
                .size();

        if (numFailed > 0 && numFailed == seqAnalysisQcRecords.size()) {
            // If all qc statuses have been failed, then this is failed
            return QcStatus.FAILED.toString();
        }

        List<String> uniqueStatuses = new ArrayList<>(new HashSet(seqQcStatuses));
        return String.join(",", uniqueStatuses);
    }

    // A Standard ExemplarSampleStatus is composed of a workflow status prefix (below) and workflow name
    public static final String WORKFLOW_STATUS_IN_PROCESS = "In Process - ";
    public static final String WORKFLOW_STATUS_READY_FOR = "Ready for - ";
    public static final String WORKFLOW_STATUS_FAILED = "Failed - ";
    public static final String WORKFLOW_STATUS_COMPLETED = "Completed - ";
    public static final String[] WORKFLOW_PROGRESS_STATUSES = new String[]{
            WORKFLOW_STATUS_READY_FOR,
            WORKFLOW_STATUS_IN_PROCESS,
            WORKFLOW_STATUS_COMPLETED,
            WORKFLOW_STATUS_FAILED
    };
    private static final Log LOGGER = LogFactory.getLog(StatusTrackerConfig.class);
    // This flag is used to indicate if a a stage is complete when that workflow has been completed
    private static final String IS_COMPLETE_FIELD = "LIMS_COMPLETE_STATUS";
    // Many Workflows -> One Stage. Maps workflows to the stage they belong to
    private static final Map<String, LimsStage> workflowNameToStageMap = new HashMap<>();

    /**
     * Retrieving the workflowList, workflow names, & categories requires DB access and only needs to be done once. This
     * ensures that the population is only done once. When called for the first time, the workflow map is computed from
     * the input connection to the DB.
     *
     * @param conn
     * @return
     */
    private static synchronized Map<String, LimsStage> computeIfAbsentWorkflowMap(ConnectionLIMS conn) {
        if (workflowNameToStageMap.size() == 0) {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();

            Set<String> validStages = new HashSet<>(Arrays.asList(STAGE_ORDER));
            try {
                List<Workflow> workflowList = vConn.getDataMgmtServer().getWorkflowManager(user).getLatestWorkflowList(user);

                // Create the mapping of the workflow name to its corresponding stage, stored in category
                for (Workflow wkflw : workflowList) {
                    // LIMS workflow's name is by default initialized on creation in LIMS, but the workflow creator
                    // should also be responsible for populating the "Short Description" field with the desired stage
                    String wkflwName = wkflw.getWorkflowName();

                    String stageName = wkflw.getShortDesc();
                    Boolean isComplete = isCompleteWorkflow(wkflw, stageName);
                    LimsStage stage = new LimsStage(stageName, isComplete);
                    if (!validStages.contains(stageName)) {
                        // TODO - Send alert
                        LOGGER.error(String.format("%s is not recognized as a valid stage. Please amend or remove", stageName));
                    }

                    workflowNameToStageMap.put(wkflwName, stage);
                }
            } catch (RemoteException | ServerException e) {
                LOGGER.error("Could not fetch Lims Stage Name - Unable to get dataManagement Server");
            }
        }
        return workflowNameToStageMap;
    }

    /**
     * Returns whether the input workflow is a complete one
     *
     * @param wkflw
     * @return
     */
    private static Boolean isCompleteWorkflow(Workflow wkflw, String stage) {
        Map<String, String> options = new HashMap<>();
        try {

            options = wkflw.getWorkflowOptions();
        } catch (RemoteException e) {
            LOGGER.error(String.format("Unable to retrieve workflow options for %s", stage));
        }
        // Default is "Complete" as most workflows are. If the LIMS workflow creator hasn't specified, make it complete
        String isCompleteString = options.computeIfAbsent(IS_COMPLETE_FIELD, k -> "true");
        return Boolean.parseBoolean(isCompleteString);
    }

    /**
     * Returns the LIMs stage as derived from the category field of the workflow the input status maps to
     *      e.g. "Illumina Sequencing" -> LimsStage instance ("Sequencing")
     *
     * NOTE - This does NOT return the dataQc stage, which requires
     * @param conn
     * @param status
     * @return
     */
    public static LimsStage getLimsStageFromStatus(ConnectionLIMS conn, String status) {
        String workflowName = getWorkflowNameFromStatus(status);
        Map<String, LimsStage> workflowMap = computeIfAbsentWorkflowMap(conn);
        if (workflowMap.containsKey(workflowName)) {
            return workflowMap.get(workflowName);
        }
        LOGGER.warn(String.format("Stage (Short Description) for Exemplar status not found: %s", status));
        return new LimsStage(STAGE_AWAITING_PROCESSING, false);
    }

    /**
     * Extracts the workflow from the input Exemplar Status
     * The Exemplar Status is typically composed of,
     *      1) a Progress Status - "Ready For" -> "In Process" -> "Completed"
     *      2) Workflow Name
     * So we need to extract the workflow name from the status prefix
     *      E.g. "Ready For - Illumina Sequencing" -> "Illumina Sequencing"
     *
     * @param exemplarSampleStatus, Sample::ExemplarSampleStatus
     * @return Workflow name
     */
    private static String getWorkflowNameFromStatus(String exemplarSampleStatus) {
        for (String progressStatus : WORKFLOW_PROGRESS_STATUSES) {
            String[] workflowStatus = exemplarSampleStatus.split(progressStatus);
            if (workflowStatus.length == 2) {
                // This will be equal 2 if one of the valid progress statuses is in the exemplar status
                return workflowStatus[1];
            }
        }
        LOGGER.warn(String.format("Non-standard Exemplar Status: %s", exemplarSampleStatus));
        return exemplarSampleStatus;
    }
}