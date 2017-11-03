
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

import org.mskcc.limsrest.staticstrings.Messages;

import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxStandalone;
import com.velox.sapioutils.client.standalone.VeloxStandaloneException;
import com.velox.sapioutils.client.standalone.VeloxStandaloneManagerContext;
import com.velox.sapioutils.client.standalone.VeloxExecutable;

import com.velox.sloan.cmo.utilities.SloanCMOUtils;


/**
 * A queued task that takes a request id and map of values and sets them 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class SetPairing  extends LimsTask 
{
  String igoUser;
  String requestId;
  String tumorId;
  String normalId;
  String tIgoId;
  String nIgoId;

  public void init(String igoUser, String requestId, String tumorId, String normalId, String tumorIgoId, String normalIgoId){ 
    this.igoUser = igoUser;
    this.requestId = requestId; 
    this.tumorId = tumorId;
    this.normalId = normalId;
    this.tIgoId = tumorIgoId;
    this.nIgoId = normalIgoId;
  }
 //execute the velox call

@PreAuthorize("hasRole('ADMIN')")
@Override
 public Object execute(VeloxConnection conn){
  String recordId = "";
  try {
    if(requestId == null || requestId.equals("")){
       throw new LimsException("Must have a request id to set the request");
    }
    List<DataRecord> matchedRequest = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId +  "'", user);
    if(matchedRequest.size() == 0){
       throw new LimsException("No Request record in the lims matches " + requestId);
    }
    DataRecord[] children = matchedRequest.get(0).getChildrenOfType("Sample", user);
    String tumorIgoId = "";
    String normalIgoId = "";
    for(DataRecord sample : children){
        if(normalId != null && tumorId != null){ 
            String cmoInfoId = "";
            DataRecord[] cmoInfos = sample.getChildrenOfType("SampleCMOInfoRecords", user);
            if(cmoInfos.length > 0){
               try{ cmoInfoId = cmoInfos[0].getStringVal("CorrectedCMOID", user); } catch(NullPointerException npe){}
            } else{
               try{ cmoInfoId = sample.getStringVal("OtherSampleId", user); } catch(NullPointerException npe){}
            }
            if(tumorId.equals(cmoInfoId)){
               try{
                   tumorIgoId = sample.getStringVal("SampleId", user);
               } catch(NullPointerException npe){
                  throw new LimsException("Sample matching " + cmoInfoId + " has no sampled id. This is a critically bad condition in the lims");
               }
               if(!"Tumor".equals(sample.getPickListVal("TumorOrNormal", user))){
                  throw new LimsException(tumorId + " is not a tumor");
               }
            } else if(normalId.equals(cmoInfoId)){
               try{
                   normalIgoId = sample.getStringVal("SampleId", user);
               } catch(NullPointerException npe){
                   throw new LimsException("Sample matching " + cmoInfoId + " has no sampled id. This is a critically bad condition in the lims");
               }
               if(!"Normal".equals(sample.getPickListVal("TumorOrNormal", user))){
                   throw new LimsException(normalId + " is not a normal");
               }
            }
        } else if(nIgoId != null && tIgoId != null){
             if(nIgoId.equals(sample.getStringVal("SampleId", user))){
                normalIgoId = nIgoId;
                if(!"Normal".equals(sample.getPickListVal("TumorOrNormal", user))){
                   throw new LimsException(normalId + " is not a normal");
                }
             } else if(tIgoId.equals(sample.getStringVal("SampleId", user) )){
                tumorIgoId = tIgoId; 
                 if(!"Tumor".equals(sample.getPickListVal("TumorOrNormal", user))){
                    throw new LimsException(tumorId + " is not a tumor");
                 }
             }
        }
     }
     if(tumorIgoId.equals("")){
         throw new LimsException(tumorId + "/" + tIgoId+ " does not match any samples in " + requestId);
     }
     if(normalIgoId.equals("")){
         throw new LimsException(normalId + "/" + nIgoId +" does not match any samples in " + requestId);
     }
     List<DataRecord> matchedPairing =  dataRecordManager.queryDataRecords("PairingInfo", "TumorId = '" + tumorIgoId +  "'", user);
     HashMap<String, Object> pairingFields = new HashMap<>();
     if(matchedPairing.size() == 0){
        pairingFields.put("TumorId", tumorIgoId);
        pairingFields.put("NormalId", normalIgoId);
        DataRecord pair = matchedRequest.get(0).addChild("PairingInfo", pairingFields, user);
        recordId =  Long.toString(pair.getRecordId()); 
     } else{
        pairingFields.put("NormalId", normalIgoId);
        matchedPairing.get(0).setFields(pairingFields, user);
         recordId = Long.toString(matchedPairing.get(0).getRecordId());
    }
    dataRecordManager.storeAndCommit("Set pairing info for " + tumorIgoId + " to " + normalIgoId, user);

  } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
           return Messages.ERROR_IN +  " setting pairing: " + e.getMessage() ;   
  
  }

  return recordId; 
 }

}
