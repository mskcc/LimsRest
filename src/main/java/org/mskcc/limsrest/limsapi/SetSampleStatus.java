
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


/**
 * A queued task that takes a sample id and a flowcellid, lane pair and if no sample has a barcode conflict with the new lane, reassigns the pool to that lane
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class SetSampleStatus  extends LimsTask 
{
  String status; 
  String sampleId; 
  String igoUser;


  public void init(String sampleId, String status, String igoUser){ 
    this.sampleId = sampleId;
    this.status = status;
    this.igoUser = igoUser;  
  }
 //execute the velox call
@PreAuthorize("hasRole('ADMIN')")
@Override
 public Object execute(VeloxConnection conn){
  String finalStatus = status;
  try { 

     List<DataRecord> samples = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + sampleId +  "'", user);
    
    if(samples.size() != 1){
        return "ERROR: This service must match exactly one sample";
    }
    DataRecord sample = samples.get(0);
    String oldStatus = "";
    try{
     oldStatus = sample.getSelectionVal("ExemplarSampleStatus", user);
    }catch(NullPointerException npe){}
    if(oldStatus.equals("")){
     sample.setDataField("ExemplarSampleStatus", status, user);
     List<DataRecord> assigned =  dataRecordManager.queryDataRecords("AssignedProcess", "SampleId = '" + sampleId +  "'", user);
     if(assigned.size() != 1){
        return "Failed to set status for " + sampleId + " because it maps to multiple assigned Processeses";
      }
     DataRecord[] childSamples = assigned.get(0).getChildrenOfType("Sample", user);
     if(childSamples.length == 0){
         assigned.get(0).addChild(sample, user);      
     }
     dataRecordManager.storeAndCommit(igoUser  + " set status of sample " + sampleId + " to " + status, user);
    }else{
       finalStatus = oldStatus;
    }

  } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
           return "ERROR IN FIXING STATUS: " + e.getMessage() + "TRACE: " + sw.toString();   
  
  }
  if(status.equals(finalStatus)){
    return "Sample "+ sampleId + " has status " + status; 
  } else{
    return "Sample " + sampleId + "has status unchanged";
  }
 }

}
