package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;


import org.mskcc.limsrest.staticstrings.Messages;
import org.mskcc.limsrest.limsapi.*;
import org.mskcc.limsrest.connection.*;

import java.io.StringWriter;
import java.io.PrintWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


@RestController
public class PairingInfo {


    private final ConnectionQueue connQueue; 
    private final SetPairing task;
    private Log log = LogFactory.getLog(PairingInfo.class);
   
    public PairingInfo( ConnectionQueue connQueue, SetPairing setter){
        this.connQueue = connQueue;
        this.task = setter;
    }


    @RequestMapping(value="/pairingInfo",  method = RequestMethod.POST)
    public ResponseEntity<String> getContent(@RequestParam(value="request") String request, @RequestParam(value="igoUser") String igoUser, 
                           @RequestParam(value="user") String user, 
                           @RequestParam(value="tumorId", required=false) String tumorId, @RequestParam(value="normalId", required=false) String normalId,
                           @RequestParam(value="tumorIgoId", required=false) String tumorIgoId, @RequestParam(value="normalIgoId", required=false) String normalIgoId){
        
       Whitelists wl = new Whitelists();
       if(!wl.textMatches(igoUser))
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("igoUser is not using a valid format. " + wl.requestFormatText());
       if(!wl.requestMatches(request)){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("requestId is not using a valid format. " + wl.requestFormatText()); 
        } 
        if(tumorId != null && !wl.textMatches(tumorId)){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("tumorId is not using a valid format. " + wl.textFormatText());
        }
        if(normalId != null && !wl.textMatches(normalId)){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("normalId is not using a valid format. " + wl.textFormatText());
        }
        if(tumorIgoId != null && !wl.sampleMatches(tumorIgoId)){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("tumorIgoId is not using a valid format. " + wl.sampleFormatText());
        }
        if(normalIgoId != null && !wl.sampleMatches(normalIgoId)){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("normalIgoId is not using a valid format. " + wl.sampleFormatText());
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

