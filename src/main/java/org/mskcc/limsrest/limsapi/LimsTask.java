package org.mskcc.limsrest.limsapi;


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
import org.mskcc.limsrest.limsapi.assignedprocess.QcStatus;
import org.mskcc.limsrest.staticstrings.Messages;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.mskcc.util.CommonUtils.runAndCatchNpe;

/**
 * This is the base class for tasks that run through the connection queue.
 * Preferred way to interact with the lims to avoid collisions on the bicapi user
 *
 * @author Aaron Gabow
 */
public class LimsTask implements VeloxExecutable<Object>, Callable<Object> {
    protected VeloxConnection velox_conn;
    protected User user;
    protected DataRecordManager dataRecordManager;
    protected DataMgmtServer dataMgmtServer;
    protected VeloxStandaloneManagerContext managerContext;

    private Log log = LogFactory.getLog(LimsTask.class);

    public LimsTask() {
    }

    public void setVeloxConnection(VeloxConnection conn) {
        velox_conn = conn;
    }

    //put it in the completion service
    @Override
    public Object call() throws Exception {
        Object result;
        velox_conn.open();
        try {
            if (velox_conn.isConnected()) {
                user = velox_conn.getUser();
                dataRecordManager = velox_conn.getDataRecordManager();
                dataMgmtServer = velox_conn.getDataMgmtServer();
                managerContext = new VeloxStandaloneManagerContext(user, dataMgmtServer);
            } else {
                log.info("the lims task has a null connection");
            }
            result = VeloxStandalone.run(velox_conn, this);
        } finally {
            velox_conn.close();
        }
        return result;
    }

    @Override
    public Object execute(VeloxConnection conn) {
        RequestSummary rs = new RequestSummary("Empty");
        return rs;
    }

    public SampleQcSummary annotateQcSummary(DataRecord qc) {

        SampleQcSummary qcSummary = new SampleQcSummary();
        try {
            Map<String, Object> qcFields = qc.getFields(user);

            String qcStatus = (String) qcFields.get("SeqQCStatus");
            log.info("Building QC record with status: " + qcStatus);
            //qcFields.forEach((key, value) -> System.out.println(key + ":" + value));

            Boolean passedQc = (Boolean) qcFields.getOrDefault("PassedQc", Boolean.FALSE);
            if (passedQc == null)
                passedQc = Boolean.FALSE;
            if (passedQc && "Passed".equals(qcStatus))
                qcSummary.setQcStatus(QcStatus.IGO_COMPLETE.getText());
            else
                qcSummary.setQcStatus(qcStatus);

            runAndCatchNpe(() -> qcSummary.setRecordId((Long) qcFields.get("RecordId")));
            runAndCatchNpe(() -> qcSummary.setSampleName((String) qcFields.get("OtherSampleId")));
            runAndCatchNpe(() -> qcSummary.setBaitSet((String) qcFields.get("BaitSet")));
            runAndCatchNpe(() -> qcSummary.setMskq((Double) qcFields.get("Mskq")));
            runAndCatchNpe(() -> qcSummary.setMeanTargetCoverage((Double) qcFields.get("MeanTargetCoverage")));
            runAndCatchNpe(() -> qcSummary.setMEAN_COVERAGE((Double) qcFields.get("MeanCoverage")));
            runAndCatchNpe(() -> qcSummary.setPercentAdapters((Double) qcFields.get("PercentAdapters")));
            runAndCatchNpe(() -> qcSummary.setPercentDuplication((Double) qcFields.get("PercentDuplication")));
            runAndCatchNpe(() -> qcSummary.setPercentOffBait((Double) qcFields.get("PercentOffBait")));
            runAndCatchNpe(() -> qcSummary.setPCT_EXC_MAPQ((Double) qcFields.get("PercentExcMapQ")));
            runAndCatchNpe(() -> qcSummary.setPCT_EXC_DUPE((Double) qcFields.get("PercentExcDupe")));
            runAndCatchNpe(() -> qcSummary.setPCT_EXC_BASEQ((Double) qcFields.get("PercentExcBaseQ")));
            runAndCatchNpe(() -> qcSummary.setPCT_EXC_TOTAL((Double) qcFields.get("PercentExcTotal")));
            runAndCatchNpe(() -> qcSummary.setPercentTarget10x((Double) qcFields.get("PercentTarget10X")));
            runAndCatchNpe(() -> qcSummary.setPercentTarget30x((Double) qcFields.get("PercentTarget30X")));
            runAndCatchNpe(() -> qcSummary.setPercentTarget40x((Double) qcFields.get("PercentTarget40X")));
            runAndCatchNpe(() -> qcSummary.setPercentTarget80x((Double) qcFields.get("PercentTarget80X")));
            runAndCatchNpe(() -> qcSummary.setPercentTarget100x((Double) qcFields.get("PercentTarget100X")));
            runAndCatchNpe(() -> qcSummary.setReadsDuped((Long) qcFields.get("ReadPairDupes")));
            runAndCatchNpe(() -> qcSummary.setReadsExamined((Long) qcFields.get("ReadsExamined")));
            runAndCatchNpe(() -> qcSummary.setTotalReads((Long) qcFields.get("TotalReads")));
            runAndCatchNpe(() -> qcSummary.setUnmapped((Long) qcFields.get("UnmappedDupes")));
            runAndCatchNpe(() -> qcSummary.setUnpairedReadsExamined((Long) qcFields.get("UnpairedReads")));
            runAndCatchNpe(() -> qcSummary.setZeroCoveragePercent((Double) qcFields.get("ZeroCoveragePercent")));
            runAndCatchNpe(() -> qcSummary.setRun((String) qcFields.get("SequencerRunFolder")));
            runAndCatchNpe(() -> qcSummary.setReviewed((Boolean) qcFields.get("Reviewed")));
            runAndCatchNpe(() -> qcSummary.setPercentRibosomalBases((Double) qcFields.get("PercentRibosomalBases")));
            runAndCatchNpe(() -> qcSummary.setPercentCodingBases((Double) qcFields.get("PercentCodingBases")));
            runAndCatchNpe(() -> qcSummary.setPercentUtrBases((Double) qcFields.get("PercentUtrBases")));
            runAndCatchNpe(() -> qcSummary.setPercentIntronicBases((Double) qcFields.get("PercentIntronicBases")));
            runAndCatchNpe(() -> qcSummary.setPercentIntergenicBases((Double) qcFields.get("PercentIntergenicBases")));
            runAndCatchNpe(() -> qcSummary.setPercentMrnaBases((Double) qcFields.get("PercentMrnaBases")));
        } catch (Throwable e) {
            log.info(e.getMessage(), e);
            qcSummary.setSampleName(Messages.ERROR_IN + " Annotation:" + e.getMessage());
        }
        return qcSummary;
    }

    public void annotateRequestSummary(RequestSummary rs, DataRecord request) {
        try {
            Map<String, Object> requestFields = request.getFields(user);
            annotateRequestSummary(rs, requestFields);
        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info(e.getMessage());
            log.info(sw.toString());
            rs.setInvestigator(Messages.ERROR_IN + " Annotation:" + e.getMessage());
        }
    }

    public void annotateRequestSummary(RequestSummary rs, Map<String, Object> requestFields) {
        try {
            runAndCatchNpe(() -> rs.setPi((String) requestFields.get("LaboratoryHead")));
            runAndCatchNpe(() -> rs.setInvestigator((String) requestFields.get("Investigator")));
            runAndCatchNpe(() -> rs.setPiEmail((String) requestFields.get("LabHeadEmail")));
            runAndCatchNpe(() -> rs.setInvestigatorEmail((String) requestFields.get("Investigatoremail")));
            runAndCatchNpe(() -> rs.setAutorunnable((Boolean) requestFields.get("BicAutorunnable")));
            runAndCatchNpe(() -> rs.setAnalysisRequested((Boolean) requestFields.get("BICAnalysis")));
            runAndCatchNpe(() -> rs.setCmoProject((String) requestFields.get("CMOProjectID")));
            runAndCatchNpe(() -> rs.setProjectManager((String) requestFields.get("ProjectManager")));
        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info(e.getMessage());
            log.info(sw.toString());
            rs.setInvestigator("Annotation failed:" + e.getMessage());
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
            runAndCatchNpe(() -> requestDetailed.setCmoPiName((String) requestFields.get("PIFirstName") + " " +
                    (String) requestFields.get("PILastName")));
            runAndCatchNpe(() -> requestDetailed.setCmoPiEmail((String) requestFields.get("PIemail")));
            runAndCatchNpe(() -> requestDetailed.setCmoProjectId((String) requestFields.get("CMOProjectID")));
            runAndCatchNpe(() -> requestDetailed.setRequestId((String) requestFields.get("RequestId")));
            runAndCatchNpe(() -> requestDetailed.setFaxNumber((String) requestFields.get("FaxNum")));
            runAndCatchNpe(() -> requestDetailed.setMailTo((String) requestFields.get("MailTo")));
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
            runAndCatchNpe(() -> requestDetailed.setFurthestSample((String) requestFields.get("FurthestSample")));
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

    public void annotateSampleSummary(SampleSummary ss, DataRecord sample) {
        try {
            Map<String, Object> sampleFields = sample.getFields(user);
            annotateSampleSummary(ss, sampleFields);
        } catch (Throwable e) {
            ss.addCmoId("Annotation failed:" + e.getMessage());
        }
    }

    public void annotateSampleSummary(SampleSummary ss, Map<String, Object> sampleFields) {
        try {
            ss.setRecordId((Long) sampleFields.get("RecordId"));
            ss.setSpecies((String) sampleFields.get("Species"));
            runAndCatchNpe(() -> ss.setRecipe((String) sampleFields.get("Recipe")));
            runAndCatchNpe(() -> ss.setTumorOrNormal((String) sampleFields.get("TumorOrNormal")));
            runAndCatchNpe(() -> ss.setTumorType((String) sampleFields.get("TumorType")));
            runAndCatchNpe(() -> ss.setGender((String) sampleFields.get("Gender")));
            ss.addRequest((String) sampleFields.get("RequestId"));
            ss.addBaseId((String) sampleFields.get("SampleId"));
            ss.addCmoId((String) sampleFields.get("OtherSampleId"));
            runAndCatchNpe(() -> ss.addExpName((String) sampleFields.get("UserSampleID")));
            runAndCatchNpe(() -> ss.setSpecimenType((String) sampleFields.get("SpecimenType")));
            runAndCatchNpe(() -> ss.addConcentration((Double) sampleFields.get("Concentration")));
            runAndCatchNpe(() -> ss.addConcentrationUnits((String) sampleFields.get("ConcentrationUnits")));
            runAndCatchNpe(() -> ss.addVolume((Double) sampleFields.get("Volume")));
            runAndCatchNpe(() -> ss.setPlatform((String) sampleFields.get("Platform")));
            ss.setDropOffDate((Long) sampleFields.get("DateCreated"));
        } catch (Throwable e) {
            ss.addCmoId("Annotation failed:" + e.getMessage());
        }
    }

    /**
     * put as a method in LimsTask because it reoccurs in sevaral tasks and how this record is being used keeps
     * shifting. Maybe once it stabilizes we can put this elsewhere.
     */
    public boolean cmoInfoCheck(String correctedId, DataRecord cmoInfo) {
        try {
            if (correctedId.equals(cmoInfo.getStringVal("CorrectedCMOID", user)) && !cmoInfo.getStringVal
                    ("CorrectedCMOID", user).equals("")) {
                return true;
            } else if (correctedId.equals(cmoInfo.getStringVal("OtherSampleId", user))) {
                return true;
            }
        } catch (NullPointerException npe) {
        } catch (NotFound | RemoteException e) {
        }
        return false;
    }
}
