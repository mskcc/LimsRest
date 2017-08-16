
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
 * A queued task that takes a request id and returns all some request information and some sample information 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class  ToggleSampleQcStatus  extends LimsTask 
{
  long recordId;
  String status;
  String requestId;
  String sampleId;
  String correctedSampleId;
  String run;
  String analyst;
  String note;
  boolean isSeqQc = true;
  private Log log = LogFactory.getLog(ToggleSampleQcStatus.class);

  public void init(long recordId, String status, String requestId, String correctedSampleId, String run, String qcType, String analyst, String note){
   this.recordId = recordId;
   this.status = status;
   this.requestId = requestId;
   //this.sampleId = sampleOtherId;
   this.correctedSampleId = (correctedSampleId == null)? "" :  correctedSampleId;
   this.run = run;
   this.analyst = analyst;
   this.note = note;
   if("Post".equals(qcType)){
      isSeqQc = false;
   }
  }

 //execute the velox call
@PreAuthorize("hasRole('USER')")
@Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
  try { 
    if(isSeqQc){
        DataRecord seqQc = dataRecordManager.querySystemForRecord( recordId, "SeqAnalysisSampleQC", user);
        seqQc.setDataField("SeqQCStatus", status, user);
        dataRecordManager.storeAndCommit("SeqAnalysisSampleQC updated to " + status , user);
    } else{
        List<DataRecord> request = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
        if(request.size() != 1){
           return "Invalid Request Id";
        }
        DataRecord[] childSamples = request.get(0).getChildrenOfType("Sample", user);
        DataRecord matchedSample = null;
        //first try to resolve through cmo info records. Only after this try to resolve directly off samples names.
        //we must do it this was because otherwise if the cmo info records resolve a sample swap we could miss it
        for(int i = 0; i < childSamples.length; i++){
            DataRecord[] cmoInfo =childSamples[i].getChildrenOfType("SampleCMOInfoRecords" ,user);
            if(cmoInfo.length > 0 && cmoInfoCheck(correctedSampleId, cmoInfo[0])){
                matchedSample = childSamples[i];
            }
        }
        if(matchedSample == null){
            try{
                for(int i = 0; i < childSamples.length; i++){
                    if(correctedSampleId.equals(childSamples[i].getStringVal("SampleId", user))){
                        matchedSample = childSamples[i];
                    }
                }
            } catch(NullPointerException npe){}
        }
        if(matchedSample == null){
            return "Invalid corrected sample id";
        }
        //Because it's possible that the wet lab puts has children of the matched sample in multiple requests and while unlikely they could end up on the same sequencer run, we fail if this does happen 
        List<DataRecord> pqcs = matchedSample.getDescendantsOfType("PostSeqAnalysisQC", user);
        int matchCount = 0; 
        for(DataRecord pqc : pqcs){
           try{
             if(run.equals(pqc.getStringVal("SequencerRunFolder", user))){
                 matchCount++;
                 if(status != null)
                     pqc.setDataField("PostSeqQCStatus", status, user);
                 if(analyst != null)
                     pqc.setDataField("Analyst", analyst, user);
                 if(note != null){ 
                     String oldNote = "";
                     try{
                       oldNote = pqc.getStringVal("Note", user);
                     }  catch(NullPointerException npe){} 
                     pqc.setDataField("Note", oldNote + "\n" + note, user);
                }
             }
           } catch(NullPointerException npe){}
        }
        if(matchCount == 0){
          return "ERROR: Failed to match a triplet with project, sample id and run id" +  requestId + "," + sampleId + "," + run;
        }
        if(matchCount > 1){
          return "ERROR: It appears that multiple children of the sample were sequenced on the same run. Please fail sample " + requestId + ", " + correctedSampleId + " in the lims";
        }
        dataRecordManager.storeAndCommit("PostSeqAnalysisQC updated to " + status , user);
    }
  } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
         String results = "ERROR IN SETTING REQUEST STATUS: " + e.getMessage() + "TRACE: " + sw.toString();   
         return results;  
  }

  return status;
 }

}
