
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
 * A queued task that takes a sample id and other values and looks for a banked sample with that id
 * If no such sample is present but there is an appropriate request it will create it
 * The other values are then set
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class SetOrCreateBanked  extends LimsTask 
{
  String[] assay;
  String barcodePosition;
  String cellCount;
  String clinicalInfo;
  String collectionYear;
  String concentrationUnits;
  String igoUser;
  String investigator;
  String gender;
  String geneticAlterations;
  String naToExtract;
  String numTubes;
  String organism;
  String platform;
  String preservation;
  String sampleId;
  String userId;
  String sequencingReadLength;
  String specimenType;
  String spikeInGenes;
  String tissueType;
  String cancerType;
  String barcodeId;
  String recipe;
  String runType;
  String serviceId;
  String sampleType;
  String sampleClass;
  String sampleOrigin;
  String tubeId;
  String patientId;
  //String seqRequest;
  String rowPosition;
  String colPosition;
  String plateId;
  String requestedReads;
  Double estimatedPurity;
  float vol;
  double concentration;
  int rowIndex;
  long transactionId;


  public void init(String igoUser, String investigator, String sampleId,  String[] assay, String clinicalInfo, String collectionYear, String concentrationUnits, String gender, 
                     String geneticAlterations, String organism, String platform, String preservation, String specimenType, String sampleType, String sampleOrigin,
                     String sampleClass, String spikeInGenes, String tissueType, String cancerType, String barcodeId, String barcodePosition, String recipe, String runType, String serviceId, 
                     String tubeId, String patientId, String rowPos, 
                     String colPos, String plateId,  String requestedReads, String cellCount, String sequencingReadLength, String numTubes, String naToExtract, Double estimatedPurity, 
                     float vol, double concentration, int rowIndex, long transactionId){
    this.igoUser = igoUser;
    this.investigator = investigator;
    this.sampleId = sampleId;
    this.userId = sampleId;
    if(assay != null)
        this.assay = assay.clone();
    this.barcodePosition = barcodePosition;
    this.cellCount = cellCount;
    this.clinicalInfo = clinicalInfo;
    this.collectionYear = collectionYear;
    this.concentrationUnits = concentrationUnits;
    this.gender = gender;
    this.geneticAlterations = geneticAlterations;
    this.naToExtract = naToExtract;
    this.numTubes = numTubes;
    this.organism = organism;
    this.patientId = patientId;
    this.platform = platform;
    this.preservation = preservation;
    this.specimenType = specimenType;
    this.spikeInGenes = spikeInGenes;
    this.tubeId = tubeId;
    this.tissueType = tissueType;
    this.cancerType = cancerType;
    this.barcodeId = barcodeId;
    this.recipe = recipe;
    this.runType = runType;
    this.sequencingReadLength = sequencingReadLength;
    this.serviceId = serviceId;
    this.sampleType = sampleType;
    this.sampleClass = sampleClass;
    this.sampleOrigin = sampleOrigin;
    this.rowPosition = rowPos;
    this.colPosition = colPos;
    this.plateId = plateId;
    this.requestedReads = requestedReads;
    this.estimatedPurity = estimatedPurity;
    this.vol = vol;
    this.concentration = concentration;
    this.rowIndex = rowIndex;
    this.transactionId = transactionId;
  }
 //execute the velox call
@PreAuthorize("hasRole('ADMIN')")
@Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
  String recordId = "-1";
  try {
    if(sampleId == null || sampleId.equals("")){
       throw new LimsException("Must have a sample id to set banked sample");
    }
    List<DataRecord> matchedBanked = dataRecordManager.queryDataRecords("BankedSample", "OtherSampleId = '" + sampleId +  "' and ServiceId = '" + serviceId + "'", user);

    DataRecord banked;
    HashMap<String, Object> bankedFields = new HashMap<>();

    if(matchedBanked.size() < 1){
       banked = dataRecordManager.addDataRecord("BankedSample", user);
       bankedFields.put("OtherSampleId", sampleId);
       bankedFields.put("RowIndex", rowIndex);
       bankedFields.put("TransactionId", transactionId);
    } else{
       banked = matchedBanked.get(0);
    }
    
    StringBuffer sb = new StringBuffer();
    for(int i = 0; i < assay.length - 1; i++){
        sb.append("'");
        sb.append(assay[i]);
        sb.append("',");
    }
    sb.append(assay[assay.length - 1]);
    String assayString = sb.toString();
    if(requestedReads != null){
        
    }
    //default species values for recipes
    if(recipe.startsWith("IMPACT")){
        organism = "Human";
    }
    if(recipe.startsWith("M-IMPACT")){
        organism = "Mouse";
    }
    SloanCMOUtils utils = new SloanCMOUtils(managerContext);
    if(assay != null && assay.length > 0 && !"".equals(assay[0])){ bankedFields.put("Assay", assayString); }
    if(!"NULL".equals(sampleId)){ bankedFields.put("UserSampleID",userId); }
    if(!"NULL".equals(investigator)){ bankedFields.put("Investigator", investigator); } 
    if(!"NULL".equals(clinicalInfo)){ bankedFields.put("ClinicalInfo", clinicalInfo); }
    if(!"NULL".equals(collectionYear)){ bankedFields.put("CollectionYear", collectionYear); }
    if(!"NULL".equals(concentrationUnits)){ bankedFields.put("ConcentrationUnits", concentrationUnits); }
    if(!"NULL".equals(gender)){ bankedFields.put("Gender", gender); }
    if(!"NULL".equals(geneticAlterations)){ bankedFields.put("GeneticAlterations", geneticAlterations); }
    if(!"NULL".equals(platform)){ bankedFields.put("Platform", platform); }
    if(!"NULL".equals(preservation)){ bankedFields.put("Preservation", preservation); }
    if(!"NULL".equals(specimenType)){ bankedFields.put("SpecimenType", specimenType); }
    if(!"NULL".equals(sampleType)){ bankedFields.put("SampleType", sampleType); }
    if(!"NULL".equals(sampleClass)){ bankedFields.put("SampleClass", sampleClass); }
    if(!"NULL".equals(organism)){ bankedFields.put("Species", organism); }
    if(!"NULL".equals(spikeInGenes)){ bankedFields.put("SpikeInGenes", spikeInGenes); }
    if(!"NULL".equals(tissueType)){ bankedFields.put("TissueSite", tissueType); }
    if(!"NULL".equals(barcodeId)){ bankedFields.put("BarcodeId", barcodeId); }
    if(!"NULL".equals(recipe)){ bankedFields.put("Recipe", recipe); }
    if(!"NULL".equals(runType)){ bankedFields.put("RunType", runType); }
    if(!"NULL".equals(serviceId)){ bankedFields.put("ServiceId", serviceId); }
    if(!"NULL".equals(tubeId)){ bankedFields.put("TubeBarcode", tubeId); }
    if(!"NULL".equals(patientId)){ bankedFields.put("PatientId", patientId); }
    if(!"NULL".equals(rowPosition)){ bankedFields.put("RowPosition", rowPosition); }
    if(!"NULL".equals(colPosition)){ bankedFields.put("ColPosition", colPosition); }
    if(!"NULL".equals(plateId)){ bankedFields.put("PlateId", plateId); }
    if(!"NULL".equals(cellCount)){ bankedFields.put("CellCount", cellCount); }
    if(!"NULL".equals(naToExtract)) {bankedFields.put("NAtoExtract", naToExtract); }
    if(!"NULL".equals(sampleOrigin)) {bankedFields.put("SampleOrigin", sampleOrigin); }
    if(!"NULL".equals(sequencingReadLength)) {bankedFields.put("RunType", sequencingReadLength); }
    if(requestedReads != null && !"".equals(requestedReads)){ bankedFields.put("RequestedReads", requestedReads); }
    if(estimatedPurity != null && !"".equals(estimatedPurity)){ bankedFields.put("EstimatedPurity", estimatedPurity); }
    if("Normal".equals(cancerType) || "Normal".equals(sampleClass)){
       bankedFields.put("TumorOrNormal", "Normal");
    } else if(!cancerType.equals("NULL")){
       bankedFields.put("TumorOrNormal", "Tumor");
       bankedFields.put("TumorType", cancerType);
    }
    if(vol > 0.0) { banked.setDataField("Volume", vol, user);}
    if(concentration > 0.0){ banked.setDataField("Concentration", concentration, user);}
    recordId = Long.toString(banked.getRecordId());
    banked.setFields(bankedFields, user);
    AuditLog log = user.getAuditLog();
    log.stopLogging(); //because users of this service might include phi in their banked sample which will need corrected
    dataRecordManager.storeAndCommit(igoUser  + " added information to banked sample " + sampleId, user);
    log.startLogging();

  } catch (Throwable e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
           return Messages.ERROR_IN +  " SETTING BANKED SAMPLE: " + e.getMessage() + "TRACE: " + sw.toString();   
  
  }

  return recordId; 
 }

}
