package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.SetPairing;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Future;


@RestController
public class PairingInfo {

    private final ConnectionQueue connQueue; 
    private final SetPairing task;
    private final Log log = LogFactory.getLog(PairingInfo.class);
   
    public PairingInfo( ConnectionQueue connQueue, SetPairing setter){
        this.connQueue = connQueue;
        this.task = setter;
    }

    @RequestMapping(value="/pairingInfo",  method = RequestMethod.POST)
    public ResponseEntity<String> getContent(@RequestParam(value="request") String request, @RequestParam(value="igoUser") String igoUser, 
                           @RequestParam(value="user") String user, 
                           @RequestParam(value="tumorId", required=false) String tumorId, @RequestParam(value="normalId", required=false) String normalId,
                           @RequestParam(value="tumorIgoId", required=false) String tumorIgoId, @RequestParam(value="normalIgoId", required=false) String normalIgoId){

       if(!Whitelists.textMatches(igoUser))
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("igoUser is not using a valid format. " + Whitelists.requestFormatText());
       if(!Whitelists.requestMatches(request)){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("requestId is not using a valid format. " + Whitelists.requestFormatText());
        } 
        if(tumorId != null && !Whitelists.textMatches(tumorId)){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("tumorId is not using a valid format. " + Whitelists.textFormatText());
        }
        if(normalId != null && !Whitelists.textMatches(normalId)){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("normalId is not using a valid format. " + Whitelists.textFormatText());
        }
        if(tumorIgoId != null && !Whitelists.sampleMatches(tumorIgoId)){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("tumorIgoId is not using a valid format. " + Whitelists.sampleFormatText());
        }
        if(normalIgoId != null && !Whitelists.sampleMatches(normalIgoId)){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("normalIgoId is not using a valid format. " + Whitelists.sampleFormatText());
        }
       log.info("starting pairing with request " + request + " tumorId " + tumorId + " normalId " + normalId + " tumorIgoId " + tumorIgoId + " normalIgoId " + normalIgoId);
       task.init(igoUser, request, tumorId, normalId, tumorIgoId, normalIgoId); 
                         
       Future<Object> result = connQueue.submitTask(task);
       String returnCode = "";
       try{
         returnCode =  (String)result.get();
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