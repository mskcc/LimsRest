
package org.mskcc.limsrest.limsapi;


import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;

import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxStandalone;
import com.velox.sapioutils.client.standalone.VeloxStandaloneException;
import com.velox.sapioutils.client.standalone.VeloxStandaloneManagerContext;
import com.velox.sapioutils.client.standalone.VeloxExecutable;

import org.mskcc.limsrest.staticstrings.Messages;

import org.mskcc.limsrest.connection.*;
import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory; 
/**
 *  This is the base class for tasks that run through the connection queue. 
 *  Preferred way to interact with the lims to avoid collisions on the bicapi user 
 * 
 * @author Aaron Gabow
 * 
 */
public class LimsTask implements VeloxExecutable<Object>, Callable<Object>
{
 protected VeloxConnection velox_conn; 
 protected User user;
 protected DataRecordManager dataRecordManager;
 protected DataMgmtServer dataMgmtServer;
 protected VeloxStandaloneManagerContext managerContext;

private Log log = LogFactory.getLog(LimsTask.class);
 public LimsTask(){
 }



  public void setVeloxConnection(VeloxConnection conn){
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
        managerContext =  new VeloxStandaloneManagerContext(user, dataMgmtServer);
      }
      else{
        log.info("the lims task has a null connection");
      }
      result = VeloxStandalone.run(velox_conn, this);
    }
     finally {
        velox_conn.close();
    }
    return result;
 }

  @Override
  public Object execute(VeloxConnection conn) {
      RequestSummary rs = new RequestSummary("Empty");
       return rs;
  }
  public void annotateQcSummary(SampleQcSummary qcSummary, DataRecord qc){
      try{
            Map<String, Object> qcFields = qc.getFields(user); 
            try{
              qcSummary.setRecordId((Long)qcFields.get("RecordId"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setSampleName((String)qcFields.get("OtherSampleId"));
            } catch(NullPointerException npe){} 
            try{          
              qcSummary.setBaitSet((String)qcFields.get("BaitSet"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setMskq((Double)qcFields.get("Mskq"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setMeanTargetCoverage((Double)qcFields.get("MeanCoverage"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setPercentAdapters((Double)qcFields.get("PercentAdapters"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setPercentDuplication((Double)qcFields.get("PercentDuplication"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setPercentOffBait((Double)qcFields.get("PercentOffBait"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setPercentTarget10x((Double)qcFields.get("PercentTarget10X"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setPercentTarget30x((Double)qcFields.get("PercentTarget30X"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setPercentTarget100x((Double)qcFields.get("PercentTarget100X"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setReadsDuped((Long)qcFields.get("ReadPairDupes"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setReadsExamined((Long)qcFields.get("ReadsExamined"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setTotalReads((Long)qcFields.get("TotalReads"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setUnmapped((Long)qcFields.get("UnmappedDupes"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setUnpairedReadsExamined((Long)qcFields.get("UnpairedReads"));
            } catch(NullPointerException npe){}
             
            try{
              qcSummary.setZeroCoveragePercent((Double)qcFields.get("ZeroCoveragePercent"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setRun((String)qcFields.get("SequencerRunFolder"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setReviewed((Boolean)qcFields.get("Reviewed"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setQcStatus((String)qcFields.get("SeqQCStatus"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setPercentRibosomalBases((Double)qcFields.get("PercentRibosomalBases"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setPercentCodingBases((Double)qcFields.get("PercentCodingBases"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setPercentUtrBases((Double)qcFields.get("PercentUtrBases"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setPercentIntronicBases((Double)qcFields.get("PercentIntronicBases"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setPercentIntergenicBases((Double)qcFields.get("PercentIntergenicBases"));
            } catch(NullPointerException npe){} 
            try{
              qcSummary.setPercentMrnaBases((Double)qcFields.get("PercentMrnaBases"));
            } catch(NullPointerException npe){}
      }
      catch(Throwable e){
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         log.info(e.getMessage());
         log.info(sw.toString());
         qcSummary.setSampleName(Messages.ERROR_IN + " Annotation:" + e.getMessage());
      }
    }
   
   public void annotateRequestSummary(RequestSummary rs, DataRecord request){
     try{
       Map<String, Object> requestFields = request.getFields(user);
       annotateRequestSummary(rs, requestFields);
     }
     catch(Throwable e){
       StringWriter sw = new StringWriter();
       PrintWriter pw = new PrintWriter(sw);
       e.printStackTrace(pw);
       log.info(e.getMessage());
       log.info(sw.toString());
       rs.setInvestigator(Messages.ERROR_IN + " Annotation:" + e.getMessage());
     }
   }

   public void annotateRequestSummary(RequestSummary rs, Map<String, Object> requestFields){
     try{
       try{ rs.setPi((String)requestFields.get("LaboratoryHead")); } catch (NullPointerException npe){}
       try{ rs.setInvestigator((String)requestFields.get("Investigator")); } catch (NullPointerException npe){}
       try{ rs.setPiEmail((String)requestFields.get("LabHeadEmail")); } catch (NullPointerException npe){}
       try{ rs.setInvestigatorEmail((String)requestFields.get("Investigatoremail")); } catch (NullPointerException npe){}
       try{ rs.setAutorunnable((Boolean)requestFields.get("BicAutorunnable")); } catch (NullPointerException npe){}
       try{ rs.setAnalysisRequested((Boolean)requestFields.get("BICAnalysis")); } catch (NullPointerException npe){}
       try{ rs.setCmoProject((String)requestFields.get("CMOProjectID")); } catch (NullPointerException npe){}
       try{ rs.setProjectManager((String)requestFields.get("ProjectManager")); } catch (NullPointerException npe){}
     }
     catch(Throwable e){
       StringWriter sw = new StringWriter();
       PrintWriter pw = new PrintWriter(sw);
       e.printStackTrace(pw);
       log.info(e.getMessage());
       log.info(sw.toString());
       rs.setInvestigator("Annotation failed:" + e.getMessage());
     }

   }

   public void annotateRequestDetailed(RequestDetailed requestDetailed, DataRecord request){
    try{
      Map<String, Object> requestFields = request.getFields(user);
      try{
      requestDetailed.setApplications((String)requestFields.get("PlatformApplication"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setBicReadme((String)requestFields.get("ReadMe"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setClinicalCorrelative((String)requestFields.get("ClinicalCorrelativeType"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setCostCenter((String)requestFields.get("CostCenter"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setFundNumber((String)requestFields.get("FundNum"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setContactName((String)requestFields.get("ContactName"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setDataAnalyst((String)requestFields.get("DataAnalyst"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setDataAnalystEmail((String)requestFields.get("DataAnalystEmail"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setDataDeliveryType((String)requestFields.get("DataDeliveryType"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setCmoContactName((String)requestFields.get("ContactName"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setCmoPiName((String)requestFields.get("PIFirstName") + " " +(String)requestFields.get("PILastName"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setCmoPiEmail((String)requestFields.get("PIemail"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setCmoProjectId((String)requestFields.get("CMOProjectID"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setRequestId((String)requestFields.get("RequestId"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setFaxNumber((String)requestFields.get("FaxNum"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setMailTo((String)requestFields.get("MailTo"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setInvestigator((String)requestFields.get("Investigator"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setIrbWaiverComments((String)requestFields.get("IRBandWaiverComments"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setPi((String)requestFields.get("LaboratoryHead"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setProjectNotes((String)requestFields.get("ProjectNotes"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setGroup((String)requestFields.get("ProcessingType"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setGroupLeader((String)requestFields.get("GroupLeader"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setInvestigatorEmail((String)requestFields.get("Investigatoremail"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setIrbId((String)requestFields.get("IRBandWaiverNumber"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setIrbVerifier((String)requestFields.get("IRBVerifier"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setPiEmail((String)requestFields.get("LabHeadEmail"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setProjectManager((String)requestFields.get("ProjectManager"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setRequestDescription((String)requestFields.get("RequestDescription"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setRequestDetails((String)requestFields.get("RequestDetail"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setRoom((String)requestFields.get("RoomNum"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setRequestType((String)requestFields.get("RequestType"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setSampleType((String)requestFields.get("SampleType"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setStatus((String)requestFields.get("Status"));
      } catch(NullPointerException npe){}
      try{
        requestDetailed.setFurthestSample((String)requestFields.get("FurthestSample"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setTelephoneNum((String)requestFields.get("TelephoneNum"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setTatFromProcessing((String)requestFields.get("TATFromInProcessing"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setTatFromReceiving((String)requestFields.get("TATFromReceiving"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setServicesRequested((String)requestFields.get("ServicesRequested"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setStudyId((String)requestFields.get("ProjectId"));
      } catch(NullPointerException npe){}
     try{
      requestDetailed.setCommunicationNotes((String)requestFields.get("PICommunication"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setCompletedDate((long)requestFields.get("CompletedDate"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setDeliveryDate((long)requestFields.get("SampleDeliveryDate"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setPartialReceivedDate((long)requestFields.get("PartiallyReceivedDate"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setReceivedDate((long)requestFields.get("ReceivedDate"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setPortalDate((long)requestFields.get("InformaticsReceipt"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setPortalUploadDate((long)requestFields.get("PortalDate"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setInvestigatorDate((long)requestFields.get("DateSentInvestigator"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setInprocessDate((long)requestFields.get("InProcessDate"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setIlabsRequestDate((long)requestFields.get("RequestStartDate"));
      } catch(NullPointerException npe){}
      try{
        requestDetailed.setIrbDate((long)requestFields.get("DateIRBandWaiverCheckout"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setSamplesReceivedDate((long)requestFields.get("RequestDate"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setAutorunnable((Boolean)requestFields.get("BicAutorunnable"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setFastqRequested((Boolean)requestFields.get("FASTQ"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setAnalysisRequested((Boolean)requestFields.get("BICAnalysis"));
      } catch(NullPointerException npe){}
      try{
      requestDetailed.setHighPriority((Boolean)requestFields.get("HighPriority"));
      } catch(NullPointerException npe){}

    } catch(Throwable e){
      requestDetailed.setInvestigator("Annotation failed: " + e.getMessage());
    }

   }

 public void annotateProjectSummary(ProjectSummary projectSummary, DataRecord project){
   try{
    Map<String, Object> projectFields = project.getFields(user);

     try{
      projectSummary.setCmoProjectId((String)projectFields.get("CMOProjectID"));
    } catch(NullPointerException npe){}

    try{
      projectSummary.setCmoProposalTitle((String)projectFields.get("CMOProposalTitle"));
    } catch(NullPointerException npe){}

    try{
      projectSummary.setCmoStudyType((String)projectFields.get("CMOStudyType"));
    } catch(NullPointerException npe){}
    try{
       projectSummary.setStudyName((String)projectFields.get("CMOStudyName"));
    } catch(NullPointerException npe){}
    try{
      projectSummary.setCmoFinalProjectTitle((String)projectFields.get("CMOFinalProjectTitle"));
    } catch(NullPointerException npe){}

    try{
      projectSummary.setCmoProjectBrief((String)projectFields.get("CMOProjectBrief"));
    } catch(NullPointerException npe){}

    try{
      projectSummary.setProjectDesc((String)projectFields.get("ProjectDesc"));
    } catch(NullPointerException npe){}

    try{
      projectSummary.setProjectName((String)projectFields.get("ProjectName"));
    } catch(NullPointerException npe){}

    try{
      projectSummary.setProjectNotes((String)projectFields.get("ProjectNotes"));
    } catch(NullPointerException npe){}

    try{
      projectSummary.setGroupLeader((String)projectFields.get("Leader"));
    } catch(NullPointerException npe){}

    try{
      projectSummary.setProjectId((String)projectFields.get("ProjectId"));
    } catch(NullPointerException npe){}

   
    try{
      projectSummary.setCmoMeetingDiscussionDate((Long)projectFields.get("CMOMeetingDiscussion"));
    } catch(NullPointerException npe){}


   } catch (Throwable e){
     projectSummary.setCmoProjectId("Annotation failed: " + e.getMessage());
   }
 }
 public void annotateSampleSummary(SampleSummary ss, DataRecord sample){
   try{
      Map<String, Object> sampleFields = sample.getFields(user);
      annotateSampleSummary(ss, sampleFields);
  }
   catch(Throwable e){
     ss.addCmoId("Annotation failed:" + e.getMessage());
   }
 }
 
 public void annotateSampleSummary(SampleSummary ss, Map<String, Object> sampleFields){
   try{
      ss.setRecordId((Long)sampleFields.get("RecordId"));
      ss.setSpecies((String)sampleFields.get("Species"));
      try{
        ss.setRecipe((String)sampleFields.get("Recipe"));
      } catch(NullPointerException npe){}
      try{
        ss.setTumorOrNormal((String)sampleFields.get("TumorOrNormal"));
       } catch(NullPointerException npe){}
      try{
        ss.setTumorType((String)sampleFields.get("TumorType"));
       } catch(NullPointerException npe){}
      try{
        ss.setGender((String)sampleFields.get("Gender"));
       } catch(NullPointerException npe){}

      ss.addRequest((String)sampleFields.get("RequestId"));
      ss.addBaseId((String)sampleFields.get("SampleId"));
      ss.addCmoId((String)sampleFields.get("OtherSampleId"));
      try{
        ss.addExpName((String)sampleFields.get("UserSampleID" ));
      } catch(NullPointerException npe){}
       try{
          ss.setSpecimenType((String)sampleFields.get("SpecimenType"));
       } catch(NullPointerException npe){}
      try{
        ss.addConcentration((Double)sampleFields.get("Concentration"));
      } catch(NullPointerException npe){}
      try{
         ss.addConcentrationUnits((String)sampleFields.get("ConcentrationUnits"));
      } catch(NullPointerException npe){}
      try{
         ss.addVolume((Double)sampleFields.get("Volume"));
      } catch(NullPointerException npe){}
      try{
         ss.setPlatform((String)sampleFields.get("Platform"));
      } catch(NullPointerException npe){}
      ss.setDropOffDate((Long)sampleFields.get("DateCreated"));
  }
   catch(Throwable e){
     ss.addCmoId("Annotation failed:" + e.getMessage());
   }
 }

 public void annotateSampleDetailed(SampleSummary ss, DataRecord sample){
   try{
      Map sampleFields = sample.getFields(user);
      ss.setRecordId((Long)sampleFields.get("RecordId"));
      if(sampleFields.containsKey("Organism") && sampleFields.get("Organism") != null && !sampleFields.get("Organism").equals("")){
         ss.setSpecies((String)sampleFields.get("Organism"));
      } else if(sampleFields.containsKey("Species")){
         ss.setSpecies((String)sampleFields.get("Species"));
      }
      ss.setAssay((String)sampleFields.get("Assay"));
      ss.setClinicalInfo((String)sampleFields.get("ClinicalInfo"));
      ss.setCollectionYear((String)sampleFields.get("CollectionYear"));
      if(sampleFields.containsKey("TumorType")){ //not relying ob catching the error since underlying map could switch in future releases
        ss.setTumorType((String)sampleFields.get("TumorType"));
        ss.setTumorOrNormal("Tumor");
       } else{
         ss.setTumorOrNormal("Normal");
      }
      ss.setGender((String)sampleFields.get("Gender"));
      ss.addExpName((String)sampleFields.get("UserSampleID"));
      ss.setGeneticAlterations((String)sampleFields.get("GeneticAlterations"));
      ss.setPatientId((String)sampleFields.get("PatientId"));
      ss.setPreservation((String)sampleFields.get("Preservation"));
      ss.setSpecimenType((String)sampleFields.get("SpecimenType"));
      ss.setSpikeInGenes((String)sampleFields.get("SpikeInGenes"));
      ss.setTubeId((String)sampleFields.get("TubeBarcode"));
      ss.setTissueSite((String)sampleFields.get("TissueSite"));
      ss.addRequest((String)sampleFields.get("RequestId"));
      ss.addCmoId((String)sampleFields.get("OtherSampleId"));
      try{ ss.addConcentration((Double)sampleFields.get("Concentration")); } catch(NullPointerException npe){}
      ss.addConcentrationUnits((String)sampleFields.get("ConcentrationUnits"));
      try{ ss.addVolume((Double)sampleFields.get("Volume")); } catch(NullPointerException npe) {}
      try{ ss.setEstimatedPurity((Double)sampleFields.get("EstimatedPurity")); } catch(NullPointerException npe) {}
      ss.setPlatform((String)sampleFields.get("Platform"));
      try{ ss.setDropOffDate((Long)sampleFields.get("DateCreated")); } catch(NullPointerException npe) {}
      try{ss.setServiceId((String)sampleFields.get("ServiceId")); }  catch(NullPointerException npe) {}
}
   catch(Throwable e){
   StringWriter sw = new StringWriter();
   PrintWriter pw = new PrintWriter(sw);
   e.printStackTrace(pw);
   log.info(e.getMessage() + " TRACE: " + sw.toString());
   ss.addCmoId(Messages.ERROR_IN + " Annotation:" + e.getMessage()  );
   }
 } 

  /**
  *
  * put as a method in LimsTask because it reoccurs in sevaral tasks and how this record is being used keeps shifting. Maybe once it stabilizes we can put this elsewhere.
  */
 public boolean cmoInfoCheck(String correctedId, DataRecord cmoInfo){ 
    try{
        if( correctedId.equals(cmoInfo.getStringVal("CorrectedCMOID", user)) && !cmoInfo.getStringVal("CorrectedCMOID", user).equals("")){
            return true;
        } else if(correctedId.equals(cmoInfo.getStringVal("OtherSampleId", user))){
            return true;
        }
    }catch(NullPointerException npe){}
    catch(NotFound | RemoteException e){}
    return false;
 }
}
