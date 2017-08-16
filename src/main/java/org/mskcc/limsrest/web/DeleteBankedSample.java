package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.List;
import java.util.LinkedList;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
public class DeleteBankedSample {


    private final ConnectionQueue connQueue; 
    private final DeleteBanked task;
    private Log log = LogFactory.getLog(DeleteBankedSample.class);
   
    public DeleteBankedSample( ConnectionQueue connQueue, DeleteBanked banked){
        this.connQueue = connQueue;
        this.task = banked;
    }



    @RequestMapping("/deleteBankedSample")
    public ResponseEntity<String>  getContent(@RequestParam(value="userId") String userId, @RequestParam(value="serviceId") String serviceId, @RequestParam(value="user") String user) {
       log.info("Starting to delete banked sample " + userId + " from service request " + serviceId +  " by " + user  );
       Whitelists wl = new Whitelists();
       if(!wl.sampleMatches(userId))
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("userId is not using a valid format. " + wl.sampleFormatText() );

       if(!wl.serviceMatches(serviceId))
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("serviceId is not using a valid igo ilabs request");

       
       log.info("Creating task");
       task.init(userId, serviceId); 
       log.info("Getting result");
       Future<Object> result = connQueue.submitTask(task);

       String returnCode = "";
       try{
         log.info("Waiting for result");
         returnCode =  (String)result.get();
         if(!returnCode.equals(Messages.SUCCESS)){;
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

    @RequestMapping("/deleteBankedService")
    public ResponseEntity<String>  getContent(@RequestParam(value="serviceId") String serviceId, @RequestParam(value="user") String user) {
       log.info("Starting to delete banked samples and request for the service " + serviceId +  " by " + user  );
       Whitelists wl = new Whitelists();

       if(!wl.serviceMatches(serviceId))
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("serviceId is not using a valid igo ilabs request");


       log.info("Creating task");
       task.init(serviceId);
       log.info("Getting result");
       Future<Object> result = connQueue.submitTask(task);

       String returnCode = "";
       try{
         log.info("Waiting for result");
         returnCode =  (String)result.get();
         if(!returnCode.equals(Messages.SUCCESS)){;
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

