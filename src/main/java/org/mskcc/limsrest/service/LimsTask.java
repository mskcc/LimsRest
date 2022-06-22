package org.mskcc.limsrest.service;

import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxExecutable;
import com.velox.sapioutils.client.standalone.VeloxStandalone;
import com.velox.sapioutils.client.standalone.VeloxStandaloneManagerContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;
import org.mskcc.limsrest.util.Messages;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.mskcc.limsrest.util.Utils.runAndCatchNpe;

/**
 * This is the base class for tasks that run through the connection queue.
 * Preferred way to interact with the lims to avoid collisions on the bicapi user
 *
 * @author Aaron Gabow
 */
public abstract class LimsTask implements VeloxExecutable<Object>, Callable<Object> {
    private static Log log = LogFactory.getLog(LimsTask.class);

    private ConnectionPoolLIMS p;

    protected User user;
    protected DataRecordManager dataRecordManager;
    protected DataMgmtServer dataMgmtServer;
    protected VeloxStandaloneManagerContext managerContext;

    public LimsTask() {
    }

    public void setConnectionPool(ConnectionPoolLIMS p) {
        this.p = p;
    }

    @Override
    public Object call() throws Exception {
        VeloxConnection velox_conn = p.getConnection();
        velox_conn.open();
        try {
            if (velox_conn.isConnected()) {
                user = velox_conn.getUser();
                dataRecordManager = velox_conn.getDataRecordManager();
                dataMgmtServer = velox_conn.getDataMgmtServer();
                managerContext = new VeloxStandaloneManagerContext(user, dataMgmtServer);
            } else {
                log.error("the lims task has a null connection");
            }
            return VeloxStandalone.run(velox_conn, this);
        } finally {
            velox_conn.close();
        }
    }

    public void annotateRequestDetailed(RequestDetailed requestDetailed, DataRecord request) {
        try {
            Map<String, Object> requestFields = request.getFields(user);

            runAndCatchNpe(() -> requestDetailed.setApplications((String) requestFields.get
                    ("PlatformApplication")));

            runAndCatchNpe(() -> requestDetailed.setBicReadme((String) requestFields.get("ReadMe")));

            runAndCatchNpe(() -> requestDetailed.setClinicalCorrelative((String) requestFields.get
                    ("ClinicalCorrelativeType")));

            runAndCatchNpe(() -> requestDetailed.setCostCenter((String) requestFields.get("CostCenter")));

            runAndCatchNpe(() -> requestDetailed.setFundNumber((String) requestFields.get("FundNum")));

            runAndCatchNpe(() -> requestDetailed.setContactName((String) requestFields.get("ContactName")));

            runAndCatchNpe(() -> requestDetailed.setDataAnalyst((String) requestFields.get("DataAnalyst")));

            runAndCatchNpe(() -> requestDetailed.setDataAnalystEmail((String) requestFields.get("DataAnalystEmail")));

            runAndCatchNpe(() -> requestDetailed.setDataDeliveryType((String) requestFields.get("DataDeliveryType")));
            runAndCatchNpe(() -> requestDetailed.setCmoContactName((String) requestFields.get("ContactName")));
            runAndCatchNpe(() -> requestDetailed.setCmoPiName(requestFields.get("PIFirstName") + " " +
                    requestFields.get("PILastName")));
            runAndCatchNpe(() -> requestDetailed.setCmoPiEmail((String) requestFields.get("PIemail")));
            runAndCatchNpe(() -> requestDetailed.setCmoProjectId((String) requestFields.get("CMOProjectID")));
            runAndCatchNpe(() -> requestDetailed.setRequestId((String) requestFields.get("RequestId")));
            runAndCatchNpe(() -> requestDetailed.setFaxNumber((String) requestFields.get("FaxNum")));
            runAndCatchNpe(() -> requestDetailed.setMailTo((String) requestFields.get("MailTo")));
            runAndCatchNpe(() -> requestDetailed.setDataAccessEmails((String) requestFields.get("DataAccessEmails")));
            runAndCatchNpe(() -> requestDetailed.setQcAccessEmails((String) requestFields.get("QcAccessEmails")));
            runAndCatchNpe(() -> requestDetailed.setInvestigator((String) requestFields.get("Investigator")));
            runAndCatchNpe(() -> requestDetailed.setIrbWaiverComments((String) requestFields.get
                    ("IRBandWaiverComments")));
            runAndCatchNpe(() -> requestDetailed.setPi((String) requestFields.get("LaboratoryHead")));
            runAndCatchNpe(() -> requestDetailed.setProjectNotes((String) requestFields.get("ProjectNotes")));
            runAndCatchNpe(() -> requestDetailed.setGroup((String) requestFields.get("ProcessingType")));
            runAndCatchNpe(() -> requestDetailed.setGroupLeader((String) requestFields.get("GroupLeader")));
            runAndCatchNpe(() -> requestDetailed.setInvestigatorEmail((String) requestFields.get("Investigatoremail")));
            runAndCatchNpe(() -> requestDetailed.setIrbId((String) requestFields.get("IRBandWaiverNumber")));
            runAndCatchNpe(() -> requestDetailed.setIrbVerifier((String) requestFields.get("IRBVerifier")));
            runAndCatchNpe(() -> requestDetailed.setPiEmail((String) requestFields.get("LabHeadEmail")));
            runAndCatchNpe(() -> requestDetailed.setProjectManager((String) requestFields.get("ProjectManager")));
            runAndCatchNpe(() -> requestDetailed.setRequestDescription((String) requestFields.get
                    ("RequestDescription")));
            runAndCatchNpe(() -> requestDetailed.setRequestDetails((String) requestFields.get("RequestDetail")));
            runAndCatchNpe(() -> requestDetailed.setRoom((String) requestFields.get("RoomNum")));
            runAndCatchNpe(() -> requestDetailed.setRequestType((String) requestFields.get("RequestType")));
            runAndCatchNpe(() -> requestDetailed.setSampleType((String) requestFields.get("SampleType")));
            runAndCatchNpe(() -> requestDetailed.setStatus((String) requestFields.get("Status")));
            runAndCatchNpe(() -> requestDetailed.setFurthest((String) requestFields.get("FurthestSample")));
            runAndCatchNpe(() -> requestDetailed.setTelephoneNum((String) requestFields.get("TelephoneNum")));
            runAndCatchNpe(() -> requestDetailed.setTatFromProcessing((String) requestFields.get
                    ("TATFromInProcessing")));
            runAndCatchNpe(() -> requestDetailed.setTatFromReceiving((String) requestFields.get("TATFromReceiving")));
            runAndCatchNpe(() -> requestDetailed.setServicesRequested((String) requestFields.get("ServicesRequested")));
            runAndCatchNpe(() -> requestDetailed.setStudyId((String) requestFields.get("ProjectId")));
            runAndCatchNpe(() -> requestDetailed.setCommunicationNotes((String) requestFields.get("PICommunication")));
            runAndCatchNpe(() -> requestDetailed.setCompletedDate((long) requestFields.get("CompletedDate")));
            runAndCatchNpe(() -> requestDetailed.setDeliveryDate((long) requestFields.get("SampleDeliveryDate")));
            runAndCatchNpe(() -> requestDetailed.setPartialReceivedDate((long) requestFields.get
                    ("PartiallyReceivedDate")));
            runAndCatchNpe(() -> requestDetailed.setReceivedDate((long) requestFields.get("ReceivedDate")));
            runAndCatchNpe(() -> requestDetailed.setPortalDate((long) requestFields.get("InformaticsReceipt")));
            runAndCatchNpe(() -> requestDetailed.setPortalUploadDate((long) requestFields.get("PortalDate")));
            runAndCatchNpe(() -> requestDetailed.setInvestigatorDate((long) requestFields.get("DateSentInvestigator")));
            runAndCatchNpe(() -> requestDetailed.setInprocessDate((long) requestFields.get("InProcessDate")));
            runAndCatchNpe(() -> requestDetailed.setIlabsRequestDate((long) requestFields.get("RequestStartDate")));
            runAndCatchNpe(() -> requestDetailed.setIrbDate((long) requestFields.get("DateIRBandWaiverCheckout")));
            runAndCatchNpe(() -> requestDetailed.setSamplesReceivedDate((long) requestFields.get("RequestDate")));
            runAndCatchNpe(() -> requestDetailed.setAutorunnable((Boolean) requestFields.get("BicAutorunnable")));
            runAndCatchNpe(() -> requestDetailed.setFastqRequested((Boolean) requestFields.get("FASTQ")));
            runAndCatchNpe(() -> requestDetailed.setAnalysisRequested((Boolean) requestFields.get("BICAnalysis")));
            runAndCatchNpe(() -> requestDetailed.setAnalysisType((String) requestFields.get("AnalysisType")));
            runAndCatchNpe(() -> requestDetailed.setHighPriority((Boolean) requestFields.get("HighPriority")));
        } catch (Throwable e) {
            requestDetailed.setInvestigator("Annotation failed: " + e.getMessage());
        }

    }

    public void annotateProjectSummary(ProjectSummary projectSummary, DataRecord project) {
        try {
            Map<String, Object> projectFields = project.getFields(user);

            runAndCatchNpe(() -> projectSummary.setCmoProjectId((String) projectFields.get("CMOProjectID")));
            runAndCatchNpe(() -> projectSummary.setCmoProposalTitle((String) projectFields.get("CMOProposalTitle")));
            runAndCatchNpe(() -> projectSummary.setCmoStudyType((String) projectFields.get("CMOStudyType")));
            runAndCatchNpe(() -> projectSummary.setStudyName((String) projectFields.get("CMOStudyName")));
            runAndCatchNpe(() -> projectSummary.setCmoFinalProjectTitle((String) projectFields.get
                    ("CMOFinalProjectTitle")));
            runAndCatchNpe(() -> projectSummary.setCmoProjectBrief((String) projectFields.get("CMOProjectBrief")));
            runAndCatchNpe(() -> projectSummary.setProjectDesc((String) projectFields.get("ProjectDesc")));
            runAndCatchNpe(() -> projectSummary.setProjectName((String) projectFields.get("ProjectName")));
            runAndCatchNpe(() -> projectSummary.setProjectNotes((String) projectFields.get("ProjectNotes")));
            runAndCatchNpe(() -> projectSummary.setGroupLeader((String) projectFields.get("Leader")));
            runAndCatchNpe(() -> projectSummary.setProjectId((String) projectFields.get("ProjectId")));
            runAndCatchNpe(() -> projectSummary.setCmoMeetingDiscussionDate((Long) projectFields.get
                    ("CMOMeetingDiscussion")));
        } catch (Throwable e) {
            projectSummary.setCmoProjectId("Annotation failed: " + e.getMessage());
        }
    }
}