package org.mskcc.limsrest.limsapi;


import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mskcc.limsrest.staticstrings.Messages;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.velox.sapioutils.shared.managers.DataRecordUtilManager;
import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxStandalone;
import com.velox.sapioutils.client.standalone.VeloxStandaloneException;
import com.velox.sapioutils.client.standalone.VeloxStandaloneManagerContext;
import com.velox.sapioutils.client.standalone.VeloxExecutable;
import com.velox.sapioutils.shared.managers.AliquotHelper;
import com.velox.sloan.cmo.utilities.SloanCMOUtils;
import com.velox.sloan.cmo.utilities.UuidGenerator;
import org.mskcc.limsrest.staticstrings.Messages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * A queued task that takes a user sample id and service id, checks to see if that banked sample exists, if it does, deletes it. NOTE: banked samples are not normally
 * deleted. This service is explicitly for e2e testing to avoid polluting the database with testing cruff
 *
 * @author Aaron Gabow
 * 
 */
@Service
public class DeleteBanked  extends LimsTask 
{
   String serviceId;
   String userId;
   
   private Log log = LogFactory.getLog(DeleteBanked.class);

 public void init(String userId, String serviceId){
   this.userId = userId;
   this.serviceId = serviceId;
 }
 public void init(String serviceId){
   this.serviceId = serviceId;
 }
 //execute the velox call
@PreAuthorize("hasRole('ADMIN')")
@Override
 public Object execute(VeloxConnection conn){
  if(serviceId != null && userId != null){ 
    try {
       DataRecordUtilManager drum = new DataRecordUtilManager(managerContext);
       List<DataRecord> bankedList = dataRecordManager.queryDataRecords("BankedSample", "UserSampleID = '" + userId  + "' AND ServiceId = '" + serviceId + "'", user);
       if(bankedList.size() == 0){
          throw new LimsException("No banked sample match that userId and serviceId");
       } else if(bankedList.size()  > 1){
          throw new LimsException("More than one banked sample matches that userId and serviceId. Fix within the LIMS");
       } else{
          drum.deleteRecords(bankedList, false); 
          dataRecordManager.storeAndCommit("Deleted the banked sample " + userId, user);
       }
    } catch(Exception e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw); 
        log.info(e.getMessage() + " TRACE: " + sw.toString());
        return Messages.ERROR_IN +  " DELETING BANKED SAMPLE: " + e.toString() + ": "  + e.getMessage();
    }
    return Messages.SUCCESS;
   } else if(serviceId != null ){
      try{
       String requestId = "";  
       List<DataRecord> bankedList = dataRecordManager.queryDataRecords("BankedSample", "ServiceId = '" + serviceId + "'", user);
       if(bankedList.size() == 0){
          throw new LimsException("No banked sample match that userId and serviceId");
       } else{
          try{ requestId = bankedList.get(0).getStringVal("RequestId", user);} catch(Exception e){}
       }
       DataRecordUtilManager drum = new DataRecordUtilManager(managerContext);
       if(requestId.equals("")){ 
         drum.deleteRecords(bankedList, false);
       } else{
          List<DataRecord>  requestList =  dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
       
          if(requestList.size() == 0 && !requestId.equals("") ){
             throw new LimsException("No request has that request id: " + requestId);
          } else if(requestList.size() > 1){
             throw new LimsException("More than one request matches that request id: " + requestId);
          }

          List<DataRecord> ancestorProjectList = requestList.get(0).getAncestorsOfType("Project", user);
          List<DataRecord> childSampleList = Arrays.asList(requestList.get(0).getChildrenOfType("Sample", user));
          List<DataRecord> descendantSampleList = requestList.get(0).getDescendantsOfType("Sample", user);
          if(childSampleList.size() != descendantSampleList.size()){
            throw new LimsException("Can only delete requests if the children samples have no descendants");
          }
          List<DataRecord> descendantCmoList = requestList.get(0).getDescendantsOfType("SampleCMOInfoRecords", user);
          List<DataRecord> descendantSeqReqList = requestList.get(0).getDescendantsOfType("SeqRequirement", user);
          List<DataRecord> descendantBarcodeList = requestList.get(0).getDescendantsOfType("IndexBarcode", user);
          drum.deleteRecords(descendantCmoList, false);
          drum.deleteRecords(descendantSeqReqList, false);
          drum.deleteRecords(descendantBarcodeList, false);
          drum.deleteRecords(childSampleList, false);
          drum.deleteRecords(requestList, false);
          drum.deleteRecords(ancestorProjectList, false);
          drum.deleteRecords(bankedList, false);
       }
       dataRecordManager.storeAndCommit("Deleted all related service", user);
    } catch(Exception e){
       StringWriter sw = new StringWriter();
       PrintWriter pw = new PrintWriter(sw);
       e.printStackTrace(pw);
       log.info(e.getMessage() + " TRACE: " + sw.toString());
       return Messages.ERROR_IN +  " DELETING REQUEST: " + e.toString() + ": "  + e.getMessage();
    }
    return Messages.SUCCESS;
  }
  else{  
    return Messages.FAILURE_IN + " Delete";

  }
  }
}
