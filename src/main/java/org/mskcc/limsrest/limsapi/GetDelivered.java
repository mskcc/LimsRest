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
import java.util.HashSet;
import java.util.TreeSet;
import java.util.concurrent.*;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxStandalone;
import com.velox.sapioutils.client.standalone.VeloxStandaloneException;
import com.velox.sapioutils.client.standalone.VeloxStandaloneManagerContext;
import com.velox.sapioutils.client.standalone.VeloxExecutable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A queued task that finds all requests that have been delivered in a time frame and finds all their samples, noting whether they have been delivered, failed, require more sequencing or never made it to the sequencer 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class GetDelivered extends LimsTask 
{
   private Log log = LogFactory.getLog(GetDelivered.class);
   int time = 0;
   String units = "";
   String investigator = "";
   final String ignoreTerm = "Under-Review";

   public void init(int time, String units){
        this.time = time;
        this.units = units;
        investigator = "NULL";
   }

  public void init(String investigator){
    this.investigator = investigator;
    time = 2;
    units = "w";
  }

  public void init(String investigator, int time, String units){
    this.investigator = investigator;
    this.time = time;
    this.units = units;
  }

  public void init(){
    time = -1;
    units = "w";
  }

 //execute the velox call
@PreAuthorize("hasRole('READ')")
@Override
 public Object execute(VeloxConnection conn){
    long now =  System.currentTimeMillis();
    long offset = (long)time;
    if(units.equals("m")){
        offset *= 60l * 1000;
    } else if(units.equals("h")){
        offset *= 60l * 60 * 1000;
    } else if(units.equals("w")){
        offset *=  7l * 24 * 60 * 60 *1000;
    } else{ //don't worry about anything exotic and just assume it's days
        offset *= 24l * 60 * 60 * 1000;
    }
    long searchPoint = now - offset;
    if(searchPoint < 1){
        searchPoint = 1l;
    }
    //find all runs
    List<RequestSummary> delivered = new LinkedList<>();
    try {
        AuditLog auditlog = user.getAuditLog();
        List<DataRecord> recentDeliveries =  null;
        if(time == -1){
           searchPoint = now + offset; 
           List<DataRecord> unreviewed = dataRecordManager.queryDataRecords("SeqAnalysisSampleQC", "DateCreated >  1484508629000 AND SeqQCStatus != 'Passed' AND SeqQCStatus not like 'Failed%'", user);
           List<List<DataRecord>> parentSamples = dataRecordManager.getParentsOfType(unreviewed, "Sample", user);  
           List<String> reqIds = new LinkedList<>();
           for(List<DataRecord> samples : parentSamples){
               for(DataRecord sample :samples){
                  try{
                     String possibleReqId = sample.getStringVal("RequestId", user);
                     if(!reqIds.contains(possibleReqId)){
                           log.info("Adding " + possibleReqId);
                          reqIds.add(possibleReqId );
                     }
                   }catch(NullPointerException npe){}
               }
           }
           recentDeliveries = new LinkedList<>();
           for(String rid : reqIds){
              recentDeliveries.addAll(dataRecordManager.queryDataRecords("Request", "RequestId = '" + rid + "'", user));
            }
        } else if(investigator.equals("NULL")){
            recentDeliveries = dataRecordManager.queryDataRecords("Request", "RecentDeliveryDate > " + searchPoint, user);
        } else{
            recentDeliveries = dataRecordManager.queryDataRecords("Request", "RecentDeliveryDate > " + searchPoint +  " AND (Investigatoremail = '" + investigator + "' OR LabHeadEmail = '" + investigator + "')", user);
        }
        List<List<DataRecord>> childSamples = dataRecordManager.getChildrenOfType(recentDeliveries, "Sample", user); 
        List<List<DataRecord>> childPlates = dataRecordManager.getChildrenOfType(recentDeliveries, "Plate", user);
        for(int i = 0; i < recentDeliveries.size(); i++){
            DataRecord request = recentDeliveries.get(i);
            Map<String, Object> requestFields = request.getFields(user);
            String requestId = (String)requestFields.get("RequestId");
            RequestSummary rs = new RequestSummary(requestId);
            try{ rs.setInvestigator((String)requestFields.get("Investigator")); } catch(Exception e){}
            try{ rs.setPi((String)requestFields.get("LaboratoryHead")); } catch(Exception e){}
            try{ rs.setAnalysisRequested((Boolean)requestFields.get("BICAnalysis")); } catch(Exception e){}
            try{ rs.setRequestType((String)requestFields.get("RequestName")); } catch(Exception e){}
            try{ rs.setProjectManager((String)requestFields.get("ProjectManager")); } catch(Exception e){}
            try{ rs.setSampleNumber(((Short)requestFields.get("SampleNumber")).intValue()); } catch(Exception e){ log.info(e.getMessage()); }
            List<DataRecord> childrenOfRequest = childSamples.get(i);
            List<DataRecord> childrenPlatesOfRequest = childPlates.get(i);
            List<List<DataRecord>> childPlateSamples = dataRecordManager.getChildrenOfType(childrenPlatesOfRequest, "Sample", user);        
            for(List<DataRecord> plateSamples : childPlateSamples){
                for(DataRecord plateSamp : plateSamples){
                    try{
                        if(requestId.equals(plateSamp.getStringVal("RequestId", user))){
                            childrenOfRequest.add(plateSamp);
                        }
                    } catch(NullPointerException npe){}

                }

            }
            
            List<List<DataRecord>> sampleQcs = dataRecordManager.getDescendantsOfType(childrenOfRequest, "SeqAnalysisSampleQC", user);
            List<List<Map<String, Object>>> allCorrectedFields = dataRecordManager.getFieldsForChildrenOfType(childrenOfRequest, "SampleCMOInfoRecords", user);
            List<AuditLogEntry> reqHistory = auditlog.getAuditLogHistory(request, false, user);
            for(AuditLogEntry logline : reqHistory){
                if(logline.dataFieldName.equals("RecentDeliveryDate")){
                    rs.addDeliveryDate(logline.timestamp);
                }
            }

            for(int j = 0; j < childrenOfRequest.size(); j++){
                DataRecord sample = childrenOfRequest.get(j);
                Map<String, Object> sampleFields = sample.getFields(user);
                Map<String, Object> correctedFields = null;
                if(allCorrectedFields != null && allCorrectedFields.size() > 0 && allCorrectedFields.get(j).size() > 0 ){
                  correctedFields = allCorrectedFields.get(j).get(0);
                } else{
                    correctedFields = new HashMap<>();
                    correctedFields.put("CorrectdCMOID", "");
                }
                SampleSummary ss = new SampleSummary();
                String sampleRequestId = "";
                try{
                    sampleRequestId = (String) sampleFields.get("RequestId");
                    ss.addRequest(sampleRequestId); 
                }
                catch(NullPointerException npe){}
                log.info((String)sampleFields.get("SampleId"));
                ss.addBaseId((String)sampleFields.get("SampleId"));
                ss.addCmoId((String)sampleFields.get("OtherSampleId"));
                try{ ss.setSpecies((String)sampleFields.get("Species")); } catch(NullPointerException npe){}
                try{  ss.addExpName((String)sampleFields.get("UserSampleID" )); } catch(NullPointerException npe){}  
                try{ ss.setRecipe((String)sampleFields.get("Recipe")); } catch(NullPointerException npe){} 
                ss.setCorrectedCmoId((String)correctedFields.get("CorrectedCMOID"));
               
                //because a child of the sample can enter a new request, we want to assure that this is the correct request                
                //if there are errors err on overreporting
                List<DataRecord> sampleQcsForSample = sampleQcs.get(j);
                for(int k = 0; k < sampleQcsForSample.size(); k++){
                    DataRecord qc = sampleQcsForSample.get(k);

                    Map<String, Object> qcFields = qc.getFields(user);
                    String qcRequestId = "";
                    try{ qcRequestId = (String)qcFields.get("Request");} catch(NullPointerException npe){}
                    if(sampleRequestId.equals(qcRequestId) || qcRequestId == null || qcRequestId.equals("")){
                        BasicQc qcSummary = new BasicQc();
                        try { qcSummary.setRun((String)qcFields.get("SequencerRunFolder")); } catch(NullPointerException npe){}
                        try { qcSummary.setSampleName((String)qcFields.get("OtherSampleId")); } catch(NullPointerException npe){}
                        try { qcSummary.setTotalReads((Long)qcFields.get("TotalReads")); } catch(NullPointerException npe){}
                        try{ qcSummary.setAlias((String)qcFields.get("Alias")); } catch(NullPointerException npe){}
                        qcSummary.setCreateDate((Long)qcFields.get("DateCreated"));
                        String qcStatus = (String)qcFields.get("SeqQCStatus");
                        if(qcStatus != null){
                            qcSummary.setQcStatus(qcStatus);
                            List<AuditLogEntry> qcHistory = auditlog.getAuditLogHistory(qc, false, user);
                            for(AuditLogEntry logline : qcHistory){
                                if(logline.dataFieldName.equals("SeqQCStatus")){
                                    qcSummary.putStatusEvent(logline.timestamp, logline.newValue);
                                }
                            }
                        }
                        ss.addBasicQc(qcSummary);
                    }
                } 
                rs.addSample(ss);
            }
            if(!delivered.contains(rs)){
                delivered.add(rs);
            }
        }
    } catch (Throwable e) {
      log.info(e.getMessage());
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      log.info(e.getMessage() + " TRACE: " + sw.toString());
      RequestSummary errorRs = new RequestSummary();
      delivered.add(errorRs);
  }

  return delivered; 
 }

}
