
package org.mskcc.limsrest.limsapi;


import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.mskcc.util.VeloxConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
  String[] igoIds;
  String[] pairs;
  String[] categories;
  String igoUser;
  String setName;
  String mapName;
  String baitSet;
  String recipe;
  String primeRequest;
  String[] externalSpecimens;

  public void init(String igoUser, String setName, String mapName, String[] requests, String[] igoIds,
                   String[] pairs, String[] categories, String baitSet, String primeRecipe, String primeRequest,
                   String[] externalSpecimens){
    this.igoUser = igoUser;
    this.setName = setName;
    this.mapName = mapName;
    this.baitSet = baitSet;
    this.recipe = primeRecipe;
    this.primeRequest = primeRequest;
    if(requests != null)
        this.requestIds = requests.clone();
    if(igoIds != null)
        this.igoIds = igoIds.clone();
    if(externalSpecimens != null)
        this.externalSpecimens = externalSpecimens.clone();
    if(pairs != null)
        this.pairs = pairs.clone();
    if(categories != null)
        this.categories = categories.clone();
  }
 //execute the velox call
@PreAuthorize("hasRole('ADMIN')")
@Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
  if(requestIds == null && igoIds == null && pairs == null ){
     return "FAILURE: You must specify at least one request or sample or have new pairing info";
  }
  StringBuilder errorList = new StringBuilder();
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
    if(igoIds != null){
        List<DataRecord> descSamples = parent.getDescendantsOfType("Sample", user);
        List names = dataRecordManager.getValueList(descSamples, "SampleId", user);
        for(Object name : names){
           nameSet.add((String) name);
        }
        for(String igoId : igoIds){
           nameSet.add(igoId);
        }
    }
    for(int i = 0; i < normalPairing.length; i++){
       if(!nameSet.contains(tumorPairing[i]) || !nameSet.contains(normalPairing[i])){
            return "FAILURE: Please confirm that " + tumorPairing[i] + " and "  + normalPairing[i] + " are known samples.";
       }
       DataRecord pairInfo = parent.addChild("PairingInfo", user);
       pairInfo.setDataField("TumorId", tumorPairing[i], user);
       pairInfo.setDataField("NormalId", normalPairing[i], user);
     
    }

    for(int i = 0; i < categoryKeys.length; i++){
       if(!nameSet.contains(categoryKeys[i])){
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
        allRequests.addAll(addOffRequestId(errorList));
    }

    if(igoIds != null && igoIds.length > 0) {
        allSamples.addAll(addOffIgoIds(errorList));
    }
    
    if(externalSpecimens != null){
        for(String spec : externalSpecimens){
            Map<String, Object> fields = new HashMap();
            fields.put(VeloxConstants.EXTERNAL_ID, spec);
            sampleSet.addChild(VeloxConstants.EXTERNAL_SPECIMEN, fields, user);
        }
    }

    if(errorList.length() > 0){
       throw new LimsException(errorList.toString());
    }
    sampleSet.addChildren(allRequests, user);
    sampleSet.addChildren(allSamples, user);
    if(baitSet != null) {
        sampleSet.setDataField("BaitSet", baitSet, user);
    }
    if(recipe != null){
        sampleSet.setDataField("Recipe", recipe, user);
    }
    if(primeRequest != null){
        sampleSet.setDataField("PrimeRequest", primeRequest, user );
    }


    dataRecordManager.storeAndCommit(igoUser  + " added sample set info to sample set " + setName, user);
    recordId = Long.toString(sampleSet.getRecordId());    

  } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
           return "FAILURE: " +  e.getMessage() + " TRACE:" + sw.toString();
  
  }

  return recordId; 
 }

    List<DataRecord> addOffRequestId(StringBuilder errorList) throws NotFound, IoError, RemoteException {
      String match = Arrays.stream(requestIds).map(r -> String.format("\'%s\'", r)).
                collect(Collectors.joining(",", "(", ")"));
      List<DataRecord> matchedReq = dataRecordManager.queryDataRecords("Request", "RequestId in " +
              match, user);

        validateMatch(errorList, matchedReq, requestIds, "RequestId");

        return matchedReq;
    }

    List<DataRecord> addOffIgoIds(StringBuilder errorList) throws NotFound, IoError, RemoteException {
      String match = Arrays.stream(igoIds).map(s -> String.format("\'%s\'", s)).
                    collect(Collectors.joining(",", "(", ")"));
      List<DataRecord> matchedSamples = dataRecordManager.queryDataRecords("Sample", "SampleId in " +
                    match, user);
        validateMatch(errorList, matchedSamples, igoIds, "SampleId");
        return matchedSamples;
    }

    private void validateMatch(StringBuilder errorList, List<DataRecord> matchedRecords,
                               String[] recIds, String idField) throws NotFound, RemoteException {
        if(matchedRecords.size() < recIds.length){
            Set<String> matchedIds = new HashSet<>();
            for(DataRecord matchedSample : matchedRecords){
                matchedIds.add(matchedSample.getStringVal(idField, user));
            }
            for (String recordId : recIds){
                if(!matchedIds.contains(recordId)){
                    errorList.append("FAILURE: There is no record matching requested id ").append(recordId);
                }
            }
        } else if(matchedRecords.size() > recIds.length) {
            errorList.append("FAILURE: There are more records matching those ids than is expected");
        }
    }

}
