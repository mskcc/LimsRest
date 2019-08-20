package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.RenameSample;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Future;


@RestController
@RequestMapping("/")
public class SetSampleName {
    private final static Log log = LogFactory.getLog(SetSampleName.class);
    private final ConnectionPoolLIMS conn;
    private final RenameSample task;
   
    public SetSampleName(ConnectionPoolLIMS conn, RenameSample renamer){
        this.conn = conn;
        this.task = renamer;
    }

    //userId refers to the the Sample.UserId in the lims
    @GetMapping("/setSampleName")
    public String getContent(@RequestParam(value="request") String request, @RequestParam(value="igoUser") String igoUser, 
                           @RequestParam(value="user") String user, 
                           @RequestParam(value="igoId", required=false) String igoId, @RequestParam(value="newUserId", required=false) String newUserId,
                           @RequestParam(value="newSampleId") String newSampleId){
       if (igoId == null){
          return "ERROR: Must specify igoId" ;
       } else if(igoId != null){
          log.info("Starting to rename sample " +  igoId + " by user " + user);
       }

       if (!Whitelists.sampleMatches(newSampleId))
           return "FAILURE: newSampleId is not using a valid format. " + Whitelists.sampleFormatText();
       if (!Whitelists.textMatches(igoUser))
           return "FAILURE: igoUser is not using a valid format. " + Whitelists.textFormatText();
       if (!Whitelists.requestMatches(request))
           return "FAILURE: request is not using a valid format. " + Whitelists.requestFormatText();


       task.init(igoUser, request,  igoId, newSampleId, newUserId); 
                         
       Future<Object> result = conn.submitTask(task);
       String returnCode = "";
       try{
         returnCode = "Sample Id:" + result.get();
       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          returnCode = "ERROR IN SETTING BANKED SAMPLE: " + e.getMessage() + " TRACE: " + sw.toString();
       }
       return returnCode;
    }
}