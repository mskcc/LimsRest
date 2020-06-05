package org.mskcc.limsrest.util;

import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.api.workflow.Workflow;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;

import java.rmi.RemoteException;
import java.util.*;

// Temporary mapping of statuse to their buckets
public class StatusTrackerConfig {
    private final static Log LOGGER = LogFactory.getLog(StatusTrackerConfig.class);

    // VALID STAGES
    public static final String STAGE_SUBMITTED = "Submitted";
    public static final String STAGE_SAMPLE_QC = "QualityControl";
    public static final String STAGE_EXTRACTION = "Extraction";
    public static final String STAGE_LIBRARY_PREP = "LibraryPreparaton";
    public static final String STAGE_LIBRARY_CAPTURE = "LibraryCapture";
    public static final String STAGE_SEQUENCING = "Sequencing";
    public static final String STAGE_DATA_QC = "DataQC";
    public static final String STAGE_IGO_COMPLETE = "IGOComplete";

    // AMBIGUOUS STAGES - valid stage?
    public static final String STAGE_AWAITING_PROCESSING = "awaitingProcessing";    // Stage prior to any workflow

    private static Set<String> VALID_STAGES = new HashSet<>(Arrays.asList(
            STAGE_SUBMITTED,
            STAGE_SAMPLE_QC,
            STAGE_EXTRACTION,
            STAGE_LIBRARY_CAPTURE,
            STAGE_LIBRARY_PREP,
            STAGE_SEQUENCING,
            STAGE_DATA_QC,
            STAGE_IGO_COMPLETE
    ));

    private static Map<String, String> workflowNameToStageMap = new HashMap<>();

    /**
     * Add the order of valid stages here and then the ordering map will be statically initialzed
     */
    private static String[] stageOrder = new String[]{
            // TODO - need to confirm w/ Anna/Ajay
            STAGE_SUBMITTED,
            STAGE_AWAITING_PROCESSING,
            STAGE_EXTRACTION,
            STAGE_LIBRARY_PREP,
            STAGE_LIBRARY_CAPTURE,
            STAGE_SAMPLE_QC,
            STAGE_SEQUENCING,
            STAGE_DATA_QC,  // If something needs to be re-sequenced, we want to keep the sample in data-qc
            STAGE_IGO_COMPLETE
    };

    /**
     * Retrieving the workflowList, workflow names, & categories requires DB access and only needs to be done once. This
     * ensures that the population is only done once
     * @param conn
     * @return
     */
    private static synchronized Map<String, String> getPopulatedWorkflowMap(ConnectionLIMS conn) {
        if(workflowNameToStageMap.size() == 0){
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();

            try {
                List<Workflow> workflowList = vConn.getDataMgmtServer().getWorkflowManager(user).getLatestWorkflowList(user);
                // Create the mapping of the workflow name to its corresponding stage, stored in category
                for(Workflow wkflw : workflowList){
                    // Each workflow has populated its stage in the "Short Description" field
                    String stage = wkflw.getShortDesc();
                    if(!VALID_STAGES.contains(stage)){
                        // TODO - Send alert
                        LOGGER.error(String.format("%s is not recognized as a valid stage. Please amend or remove"));
                    }
                    workflowNameToStageMap.put(wkflw.getWorkflowName(), stage);
                }
                return workflowNameToStageMap;
            } catch (RemoteException | ServerException e){
                LOGGER.error("Could not fetch Lims Stage Name - Unable to get dataManagement Server");
            }
        }
        return new HashMap<>();
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
        Map<String, String> workflowMap = getPopulatedWorkflowMap(conn);
        if(workflowMap.containsKey(workflowName)){
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
        String[] statusComponents = exemplarSampleStatus.split(" - ");
        if(statusComponents.length != 2){
            LOGGER.error("Failed to extract stage from status");
            return null;
        }
        return statusComponents[1];
    }
}