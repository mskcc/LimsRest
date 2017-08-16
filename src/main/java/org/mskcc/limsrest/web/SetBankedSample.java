package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.List;
import java.util.LinkedList;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.mskcc.limsrest.limsapi.*;
import org.mskcc.limsrest.connection.*;
import org.mskcc.limsrest.staticstrings.Messages;


import java.io.StringWriter;
import java.io.PrintWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@RestController
public class SetBankedSample {


    private final ConnectionQueue connQueue; 
    private final SetOrCreateBanked task;
    private Log log = LogFactory.getLog(SetBankedSample.class);
   
    public SetBankedSample( ConnectionQueue connQueue, SetOrCreateBanked banked){
        this.connQueue = connQueue;
        this.task = banked;
    }

//userId refers to the the Sample.UserId in the lims
    @RequestMapping("/setBankedSample")
    public ResponseEntity<String> getContent(@RequestParam(value="userId") String userId, @RequestParam(value="user") String user,
                           @RequestParam(value="igoUser") String igoUser, @RequestParam(value="index", defaultValue="NULL") String barcodeId, @RequestParam(value="barcodePosition", defaultValue="NULL") String barcodePosition,
                           @RequestParam(value="vol", defaultValue="-1.0") String vol, @RequestParam(value="estimatedPurity", required=false) String estimatedPurity,
                           @RequestParam(value="concentration", defaultValue="-1.0") String concentration, @RequestParam(value="concentrationUnits", defaultValue="NULL") String concentrationUnits,  @RequestParam(value="sequencingReadLength", defaultValue="NULL") String sequencingReadLength, @RequestParam(value="numTubes", defaultValue="NULL") String numTubes, 
                           @RequestParam(value="assay", required = false) String[] assay,  @RequestParam(value="clinicalInfo", defaultValue="NULL") String clinicalInfo,
                           @RequestParam(value="collectionYear", defaultValue="NULL") String collectionYear,  @RequestParam(value="gender", defaultValue="NULL") String gender,
                           @RequestParam(value="knownGeneticAlteration", defaultValue="NULL") String geneticAlterations, @RequestParam(value="rowIndex") String rowIndex,  
                           @RequestParam(value="transactionId") String transactionId, @RequestParam(value="organism", defaultValue="NULL") String organism,
                           @RequestParam(value="platform", defaultValue="NULL") String platform,  @RequestParam(value="preservation", defaultValue="NULL") String preservation,
                           @RequestParam(value="specimenType", defaultValue="NULL") String specimenType,  @RequestParam(value="sampleType", defaultValue="NULL") String sampleType,
                           @RequestParam(value="sampleOrigin", defaultValue="NULL") String sampleOrigin,
                           @RequestParam(value="sampleClass", defaultValue="NULL") String sampleClass, @RequestParam(value="requestedReads", required=false) String requestedReads,
                           @RequestParam(value="spikeInGenes", defaultValue="NULL") String spikeInGenes,  @RequestParam(value="tissueType", defaultValue="NULL") String tissueType,
                           @RequestParam(value="cancerType", defaultValue="Normal") String cancerType, @RequestParam(value="recipe", defaultValue="NULL") String recipe,
                           @RequestParam(value="runType", defaultValue="NULL") String runType, @RequestParam(value="investigator", defaultValue="NULL") String investigator,
                           @RequestParam(value="cellCount", defaultValue="NULL") String cellCount, @RequestParam(value="naToExtract", defaultValue="NULL") String naToExtract,
                           @RequestParam(value="serviceId") String serviceId, @RequestParam(value="coverage", defaultValue="NULL") String coverage,
                           @RequestParam(value="seqRequest", defaultValue="NULL") String seqRequest, @RequestParam(value="rowPos", defaultValue="NULL") String rowPos, 
                           @RequestParam(value="colPos", defaultValue="NULL") String colPos, @RequestParam(value="plateId", defaultValue="NULL") String plateId,
                           @RequestParam(value="tubeId", defaultValue="NULL") String tubeId, @RequestParam(value="patientId", defaultValue="NULL") String patientId){ 
       log.info("Starting to set banked sample " + userId + " by user " + user);
       if(assay == null){
          assay = new String[1];
          assay[0] = "NULL";
       }
       Double ep = null;
       Double rr = null;
       if(estimatedPurity != null){  
           try{
              ep = new Double(estimatedPurity);
           } catch(NumberFormatException nfe){
              ep = 0.0d;
           }
       }
       Whitelists wl = new Whitelists();
       if(!wl.textMatches(serviceId)){
        //response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request is not using a valid format");
        //return "FAILURE: request is not using a valid format";
       }
       if(!wl.sampleMatches(userId)){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("userId is not using a valid format"); 
        //return "FAILURE: userId is not using a valid format";
        //throw new BlacklistException( "FAILURE: userId is not using a valid format");
       }

      // if(requestedReads != null){ rr = new Double(requestedReads);}
       task.init(igoUser, investigator, userId, assay, clinicalInfo, collectionYear, concentrationUnits, gender, geneticAlterations, organism, platform, preservation, 
                        specimenType, sampleType, sampleOrigin, sampleClass, spikeInGenes, tissueType, cancerType, barcodeId, barcodePosition, recipe, runType, serviceId, tubeId, patientId,rowPos, colPos, plateId, requestedReads,
                        cellCount, sequencingReadLength, numTubes, naToExtract, ep, Float.parseFloat(vol), Double.parseDouble(concentration), Integer.parseInt(rowIndex), Long.parseLong(transactionId) );
       Future<Object> result = connQueue.submitTask(task);
       String returnCode = "";
       try{
         returnCode = (String)result.get();
         if(returnCode.startsWith(Messages.ERROR_IN)){
            throw new LimsException(returnCode);
         }
         returnCode = "Record Id:" + returnCode;
       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          returnCode =  e.getMessage() + "\nTRACE: " + sw.toString();
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(returnCode);

       }
       return ResponseEntity.ok(returnCode);
    }
}

