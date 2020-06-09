package org.mskcc.limsrest.util;

import java.rmi.RemoteException;
import java.util.*;

import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.api.workflow.Workflow;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;

// Temporary mapping of statuse to their buckets
public class StatusTrackerConfig {
    public static final String STAGE_SUBMITTED = "Submitted";
    public static final String STAGE_EXTRACTION = "Extraction";
    public static final String STAGE_LIBRARY_PREP = "Library Preparaton";
    public static final String STAGE_LIBRARY_CAPTURE = "Library Capture";
    public static final String STAGE_SAMPLE_QC = "Quality Control";
    public static final String STAGE_SEQUENCING = "Sequencing";
    public static final String STAGE_DATA_QC = "Data QC";
    public static final String STAGE_IGO_COMPLETE = "IGO Complete";
    public static final String STAGE_LIBRARY_QC = "Library QC";
    public static final String STAGE_COVID_19 = "COVID-19 Assay";
    public static final String STAGE_PCR = "Digital PCR";
    public static final String STAGE_ADDING_CMO_INFORMATION = "Adding CMO Information";
    public static final String STAGE_PENDING_USER_DECISION = "Pending User Decision";
    public static final String STAGE_COVID_19_ASSAY = "COVID-19 Assay";
    public static final String STAGE_BATCH_PLANNING = "Batch Planning";
    public static final String STAGE_SAMPLE_ALIQUOTS = "Create Sample Aliquots";
    public static final String STAGE_NANO_STRING = "Nano string";
    public static final String STAGE_STR_PCR = "STR PCR";
    public static final String STAGE_STR_ANALYSIS = "STR Analysis";
    public static final String STAGE_SNP_FINGERPRINTING = "SNP Fingerprinting";
    public static final String STAGE_SAMPLE_RECEIPT = "Sample Receipt";
    public static final String STAGE_SAMPLE_REPLACEMENT_COMBINATION = "Sample Replacement/Combination";
    public static final String STAGE_TRANSFER_TUBE_SAMPLES = "Transfer Tube Samples to Plates";
    public static final String STAGE_PATHOLOGY = "Pathology";
    public static final String STAGE_Digital_PCR = "Digital PCR";
    public static final String STAGE_RETURNED_TO_USER = "Returned to User";
    // Invalid Stage, but used when the exact stage can't be determined (e.g. manual status assignment, new workflow)
    public static final String STAGE_AMBIGUOUS = "Ambiguous Stage";
    // TODO - This should be added, but in a way that it disappears once it is not longer processing
    // TODO - PROCESSING STAGES
    public static final String STAGE_AWAITING_PROCESSING = "awaitingProcessing";    // Stage prior to any workflow
    private final static Log LOGGER = LogFactory.getLog(StatusTrackerConfig.class);
    /*
    new SampleStageTester("Awaiting Processing", "05245_16","", ""),
    new SampleStageTester("Ready for Processing", "Pool-06475_G-07532-07539-D1_1_1", "06475_G,07532,07539", ""),
    new SampleStageTester("In Processing", "06430_2", "06049_T", ""),
    new SampleStageTester("Processing Completed", "05411_97", "", "")
     */
    private static Map<String, String> workflowNameToStageMap = new HashMap<>();

    /**
     * Add the order of valid stages here and then the ordering map will be statically initialzed
     */
    final public static String[] STAGE_ORDER = new String[]{
            // TODO - need to confirm w/ Anna/Ajay
            STAGE_SUBMITTED,
            STAGE_AWAITING_PROCESSING,
            STAGE_EXTRACTION,
            STAGE_LIBRARY_PREP,
            STAGE_LIBRARY_CAPTURE,
            STAGE_LIBRARY_QC,
            STAGE_SAMPLE_QC,
            STAGE_COVID_19,
            STAGE_PCR,
            STAGE_Digital_PCR,
            STAGE_ADDING_CMO_INFORMATION,
            STAGE_PENDING_USER_DECISION,
            STAGE_COVID_19_ASSAY,
            STAGE_BATCH_PLANNING,
            STAGE_SAMPLE_ALIQUOTS,
            STAGE_NANO_STRING,
            STAGE_STR_PCR,
            STAGE_STR_ANALYSIS,
            STAGE_SNP_FINGERPRINTING,
            STAGE_SAMPLE_RECEIPT,
            STAGE_SAMPLE_REPLACEMENT_COMBINATION,
            STAGE_TRANSFER_TUBE_SAMPLES,
            STAGE_PATHOLOGY,
            STAGE_SEQUENCING,
            STAGE_DATA_QC,      // If something needs to be re-sequenced, we want to keep the sample in data-qc
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

    // A Standard ExemplarSampleStatus is composed of a workflow status prefix (below) and workflow name
    public static String WORKFLOW_STATUS_COMPLETED = "Completed - ";
    private static String WORKFLOW_STATUS_IN_PROCESS = "In Process - ";
    private static String WORKFLOW_STATUS_READY_FOR = "Ready for - ";
    private static String WORKFLOW_STATUS_FAILED = "Failed - ";
    private static String[] WORKFLOW_PROGRESS_STATUSES = new String[]{
            WORKFLOW_STATUS_READY_FOR,
            WORKFLOW_STATUS_IN_PROCESS,
            WORKFLOW_STATUS_COMPLETED,
            WORKFLOW_STATUS_FAILED
    };

    /**
     * Retrieving the workflowList, workflow names, & categories requires DB access and only needs to be done once. This
     * ensures that the population is only done once. When called for the first time, the workflow map is computed from
     * the input connection to the DB.
     *
     * @param conn
     * @return
     */
    private static synchronized Map<String, String> computeIfAbsentWorkflowMap(ConnectionLIMS conn) {
        if (workflowNameToStageMap.size() == 0) {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();

            Set<String> validStages = new HashSet<>(Arrays.asList(STAGE_ORDER));
            try {
                List<Workflow> workflowList = vConn.getDataMgmtServer().getWorkflowManager(user).getLatestWorkflowList(user);

                // Each stage should map to its stage exactly
                for(String stage : STAGE_ORDER){
                    workflowNameToStageMap.put(stage, stage);
                }

                // Create the mapping of the workflow name to its corresponding stage, stored in category
                for (Workflow wkflw : workflowList) {
                    // Each workflow has populated its stage in the "Short Description" field
                    String stage = wkflw.getShortDesc();
                    if (!validStages.contains(stage)) {
                        // TODO - Send alert
                        LOGGER.error(String.format("%s is not recognized as a valid stage. Please amend or remove", stage));
                    }
                    workflowNameToStageMap.put(wkflw.getWorkflowName(), stage);
                }
            } catch (RemoteException | ServerException e) {
                LOGGER.error("Could not fetch Lims Stage Name - Unable to get dataManagement Server");
            }
        }
        return workflowNameToStageMap;
    }

    /**
     * Returns the LIMs stage name as derived from the category field of the workflow the input status maps to
     *      e.g. "Illumina Sequencing" -> "Sequencing"
     * @param conn
     * @param status
     * @return
     */
    public static String getLimsStageNameFromStatus(ConnectionLIMS conn, String status) {
        String workflowName = getWorkflowNameFromStatus(status);
        Map<String, String> workflowMap = computeIfAbsentWorkflowMap(conn);
        if (workflowMap.containsKey(workflowName)) {
            return workflowMap.get(workflowName);
        }
        LOGGER.warn(String.format("Stage (Short Description) for Exemplar status not found: %s", status));
        return "";
    }

    /**
     * Extracts the workflow from the input Exemplar Status
     *      The Exemplar Status is typically composed of,
     *          1) a Progress Status - "Ready For" -> "In Process" -> "Completed"
     *          2) Workflow Name
     *      So we need to extract the workflow name from the status prefix
     *          E.g. "Ready For - Illumina Sequencing" -> "Illumina Sequencing"
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