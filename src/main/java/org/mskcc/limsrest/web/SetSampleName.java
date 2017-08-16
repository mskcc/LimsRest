package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.List;
import java.util.LinkedList;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import org.mskcc.limsrest.limsapi.*;
import org.mskcc.limsrest.connection.*;

import java.io.StringWriter;
import java.io.PrintWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


@RestController
public class SetSampleName {


    private final ConnectionQueue connQueue; 
    private final RenameSample task;
    private Log log = LogFactory.getLog(SetSampleName.class);
   
    public SetSampleName( ConnectionQueue connQueue, RenameSample renamer){
        this.connQueue = connQueue;
        this.task = renamer;
    }


//userId refers to the the Sample.UserId in the lims
    @RequestMapping("/setSampleName")
    public String getContent(@RequestParam(value="request") String request, @RequestParam(value="igoUser") String igoUser, 
                           @RequestParam(value="user") String user, 
                           @RequestParam(value="igoId", required=false) String igoId, @RequestParam(value="newUserId", required=false) String newUserId,
                           @RequestParam(value="newSampleId") String newSampleId){
       if(igoId == null){
          return "ERROR: Must specify igoId" ;
       } else if(igoId != null){
          log.info("Starting to rename sample " +  igoId + " by user " + user);
       } 
        
       Whitelists wl = new Whitelists();
       if(!wl.sampleMatches(newSampleId))
            return "FAILURE: newSampleId is not using a valid format. " + wl.sampleFormatText();
       if(!wl.textMatches(igoUser))
            return "FAILURE: igoUser is not using a valid format. " + wl.textFormatText();
        if(!wl.requestMatches(request)){
            return "FAILURE: request is not using a valid format. " + wl.requestFormatText();
        } 


       task.init(igoUser, request,  igoId, newSampleId, newUserId); 
                         
       Future<Object> result = connQueue.submitTask(task);
       String returnCode = "";
       try{
         returnCode = "Sample Id:" + (String)result.get();
       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          returnCode = "ERROR IN SETTING BANKED SAMPLE: " + e.getMessage() + " TRACE: " + sw.toString();
       }
       return returnCode;
    }

}

