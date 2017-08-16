
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
 * A queued task that takes requests and samples, and makes a SampleSet record in the lims with this information.
 * If there is a sample set already present with this set name, it will append the information to the existing one.
 * @author Aaron Gabow
 * 
 */
@Service
public class AddOrCreateSet  extends LimsTask 
{
  String[] requestIds;
  String[] samples;
  String[] igoIds;
  String[] pairs;
  String[] categories;
  String igoUser;
  String setName;
  String mapName;
  boolean validate;

  public void init(String igoUser, String setName, String mapName, String[] requests, String[] samples, String[] igoIds, String[] pairs, String[] categories, boolean validate){
    this.igoUser = igoUser;
    this.setName = setName;
    this.mapName = mapName;
    if(requests != null)
        this.requestIds = requests.clone();
    if(samples != null)
        this.samples = samples.clone();
    if(igoIds != null)
        this.igoIds = igoIds.clone();
    if(pairs != null)
        this.pairs = pairs.clone();
    if(categories != null)
        this.categories = categories.clone();
    this.validate = validate;
  }
 //execute the velox call
@PreAuthorize("hasRole('ADMIN')")
@Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
  if(samples == null && requestIds == null && igoIds == null && pairs == null ){
     return "FAILURE: You must specify at least one request or sample or have new pairing info";
  }
  String recordId = "-1";
  try { 
    List<DataRecord> allRequests = new LinkedList<>();
    List<DataRecord> allSamples = new LinkedList<>();
    if(pairs == null){
      pairs = new String[0];
    }
    String[] tumorPairing = new String[pairs.length];
    String[] normalPairing = new String[pairs.length];
    for(int i = 0; i < pairs.length; i++){
       String[] pairBreak = pairs[i].split(":");
       tumorPairing[i] = pairBreak[0];
       normalPairing[i] = pairBreak[1];
    }
    if(categories == null){
       categories = new String[0];
    }
    String[] categoryKeys = new String[categories.length];
    String[] categoryVals = new String[categories.length];
    for(int i = 0; i < categories.length; i++){
       String[] catBreak = categories[i].split(":");
       categoryKeys[i] = catBreak[0];
       categoryVals[i] = catBreak[1]; 
    }
 
    DataRecord parent, sampleSet = null;
    //if the service is just adding pairing and category information to an existing request
    if(requestIds != null && requestIds.length == 1 && setName == null){
        parent = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestIds[0]  + "'", user).get(0);
    
    } else{
        List<DataRecord> matchedSets = dataRecordManager.queryDataRecords("SampleSet", "Name = '" + setName  + "'", user);
        if(matchedSets.size() < 1){
            sampleSet = dataRecordManager.addDataRecord("SampleSet", user);
            sampleSet.setDataField("Name", setName, user);
        } else{
            sampleSet = matchedSets.get(0);
        }
        parent = sampleSet;
    
    }
    HashSet<String> nameSet = new HashSet<>();
    if(validate){
        List<DataRecord> descSamples = parent.getDescendantsOfType("Sample", user);
        List names = dataRecordManager.getValueList(descSamples, "SampleId", user);
        for(Object name : names){
           nameSet.add((String) name);
        }
    }
    for(int i = 0; i < normalPairing.length; i++){
       if(validate && (!nameSet.contains(tumorPairing[i]) || !nameSet.contains(normalPairing[i]))){
            return "FAILURE: Please confirm that " + tumorPairing[i] + " and "  + normalPairing[i] + " are known samples.";
       }
       DataRecord pairInfo = parent.addChild("PairingInfo", user);
       pairInfo.setDataField("TumorId", tumorPairing[i], user);
       pairInfo.setDataField("NormalId", normalPairing[i], user);
     
    }

    for(int i = 0; i < categoryKeys.length; i++){
       if(validate && !nameSet.contains(categoryKeys[i])){
            return "FAILURE: Please confirm that " + categoryKeys[i] + " is a known sample.";
       }
       DataRecord categoryMap = parent.addChild("CategoryMap", user);
       categoryMap.setDataField("OtherSampleId", categoryKeys[i], user);
       categoryMap.setDataField("Category", categoryVals[i], user);
       categoryMap.setDataField("MapName", mapName, user);
    }
    //if updating a request's pair information, we are done so save and return
    if(sampleSet == null){
        dataRecordManager.storeAndCommit(igoUser  + " added pairing and category info to request " + requestIds[0], user);
        return Long.toString(parent.getRecordId()); 
        
    }

    if(requestIds != null){
      for(int i = 0; i < requestIds.length; i++){
    
         List<DataRecord> matchedReq = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestIds[i] + "'", user);
         if(matchedReq.size() == 0){
           return "FAILURE: You have selected to add a request " + requestIds[i] + " that is not in the LIMS";
         }
         allRequests.add(matchedReq.get(0));
      }
    }
    if(samples != null){
      HashSet<String> parentRequests = new HashSet<>();

      for(int i = 0; i < samples.length; i++){
         String sampleRequest = "";
         String sampleName = "";
         try{
            sampleRequest = samples[i].split(":")[0];
            sampleName = samples[i].split(":")[1];
         } catch( ArrayIndexOutOfBoundsException aie){
            return "FAILURE: " + samples[i] + " is not of the format REQUESTID:SAMPLEID";
         }
         parentRequests.add(sampleRequest);
      }
      StringBuffer sb = new StringBuffer();
      sb.append("(");
      for(String element : parentRequests){
        sb.append(element);
        sb.append(",");

      }
      sb.insert( sb.length() - 1, ")");
      List<DataRecord> matchedRequests = dataRecordManager.queryDataRecords("Request", "RequestId in " + sb.toString(), user);  
      List<List<DataRecord>> childSamples = dataRecordManager.getChildrenOfType(matchedRequests, "Sample", user);
      HashMap<String, DataRecord> requestCorrectedSample2Sample = new HashMap<>();
      for(int i = 0; i < matchedRequests.size(); i++){
        String reqId = "";
        try{ reqId =  matchedRequests.get(i).getStringVal("RequestId", user); } catch(NullPointerException npe){};
        List<List<DataRecord>> samplesInfos = dataRecordManager.getChildrenOfType(childSamples.get(i), "SampleCMOInfoRecords", user);
        for(int j = 0; j < childSamples.get(i).size(); j++){
            List<DataRecord> infos = samplesInfos.get(j);
            if(infos.size() > 0){
                String key = reqId + ":" + infos.get(0).getStringVal("CorrectedCMOID", user);
                requestCorrectedSample2Sample.put(key,childSamples.get(i).get(j));
            }
        }
      }
      for(int i = 0; i < samples.length; i++){
        if(requestCorrectedSample2Sample.containsKey(samples[i])){
            allSamples.add(requestCorrectedSample2Sample.get(samples[i]));
        } else{
            return "FAILURE: There is no known request:sample pair with value " + samples[i] + ". Make sure you are using the correct igo id";
        }
      }
    }
    if(igoIds != null){
        for(int i = 0; i < igoIds.length; i++){
           List<DataRecord> matchedSample = dataRecordManager.queryDataRecords("Sample", "SampleId = '" + igoIds[i] + "'", user);
           if(matchedSample.size() == 0){
              return "FAILURE: You have selected to add a sample " + igoIds[i] + " that is not in the LIMS";
           }
           allSamples.add(matchedSample.get(0));
        }
    }

    for(DataRecord childReq : allRequests){
       sampleSet.addChild(childReq, user);
    }
    for(DataRecord childSamp : allSamples){
       sampleSet.addChild(childSamp, user);
    }


    dataRecordManager.storeAndCommit(igoUser  + " added sample set info to sample set " + setName, user);
    recordId = Long.toString(sampleSet.getRecordId());    

  } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
           return "ERROR IN WRITING SET INFORMATION: " + e.getMessage() + "TRACE: " + sw.toString();   
  
  }

  return recordId; 
 }

}
