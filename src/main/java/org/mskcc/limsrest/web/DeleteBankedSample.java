package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.DeleteBanked;
import org.mskcc.limsrest.limsapi.LimsException;
import org.mskcc.limsrest.staticstrings.Messages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Future;


@RestController
@RequestMapping("/")
public class DeleteBankedSample {
    private final Log log = LogFactory.getLog(DeleteBankedSample.class);
    private final ConnectionQueue connQueue; 
    private final DeleteBanked task;
   
    public DeleteBankedSample( ConnectionQueue connQueue, DeleteBanked banked){
        this.connQueue = connQueue;
        this.task = banked;
    }

    @GetMapping("/deleteBankedSample")
    public ResponseEntity<String>  getContent(@RequestParam(value="userId") String userId, @RequestParam(value="serviceId") String serviceId, @RequestParam(value="user") String user) {
       log.info("Starting to delete banked sample " + userId + " from service request " + serviceId +  " by " + user  );
       if(!Whitelists.sampleMatches(userId))
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("userId is not using a valid format. " + Whitelists.sampleFormatText() );

       if(!Whitelists.serviceMatches(serviceId))
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("serviceId is not using a valid igo ilabs request");
       
       log.info("Creating task");
       task.init(userId, serviceId); 
       log.info("Getting result");
       Future<Object> result = connQueue.submitTask(task);

       String returnCode = "";
       try{
         log.info("Waiting for result");
         returnCode =  (String)result.get();
         if(!returnCode.equals(Messages.SUCCESS)){
             throw new LimsException(returnCode);
        }
       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          returnCode =  e.getMessage() + "\nTRACE: " + sw.toString();
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(returnCode);

       }
       return ResponseEntity.ok(returnCode);
    }

    @GetMapping("/deleteBankedService")
    public ResponseEntity<String>  getContent(@RequestParam(value="serviceId") String serviceId, @RequestParam(value="user") String user) {
       log.info("Starting to delete banked samples and request for the service " + serviceId +  " by " + user  );

       if(!Whitelists.serviceMatches(serviceId))
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("serviceId is not using a valid igo ilabs request");

       log.info("Creating task");
       task.init(serviceId);
       log.info("Getting result");
       Future<Object> result = connQueue.submitTask(task);

       String returnCode = "";
       try{
         log.info("Waiting for result");
         returnCode =  (String)result.get();
         if(!returnCode.equals(Messages.SUCCESS)){
             throw new LimsException(returnCode);
        }
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