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
public class PromoteBankedSample {


    private final ConnectionQueue connQueue; 
    private final PromoteBanked task;
    private Log log = LogFactory.getLog(PromoteBankedSample.class);
   
    public PromoteBankedSample( ConnectionQueue connQueue, PromoteBanked banked){
        this.connQueue = connQueue;
        this.task = banked;
    }



    @RequestMapping("/promoteBankedSample")
    public ResponseEntity<String>  getContent(@RequestParam(value="bankedId") String[] bankedId, @RequestParam(value="user") String user,
                           @RequestParam(value="requestId",  defaultValue="NULL") String request, @RequestParam(value="projectId",  defaultValue="NULL") String project, 
                           @RequestParam(value="serviceId" ) String service, @RequestParam(value="igoUser") String igoUser,
                           @RequestParam(value="dryrun", defaultValue="false") String dryrun) {
       log.info("Starting to promote banked sample " + bankedId[0] + " to request " + request + ":" + service + ":" + project + " by service " + user + " and igo user" + igoUser );
       Whitelists wl = new Whitelists();
       for(String bid : bankedId){
           if(!wl.sampleMatches(bid))
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bankedId " + bid + " is not using a valid format. " + wl.sampleFormatText() );
       }
       if(!wl.textMatches(igoUser))
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("igoUser is not using a valid format. " + wl.textFormatText());

       if(!wl.requestMatches(request))
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("requestId is not using a valid format. " + wl.requestFormatText());
    

       if(!wl.requestMatches(project))
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("projectId is not using a valid format. " + wl.requestFormatText());

      if(!wl.serviceMatches(service))
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("serviceId is not using a valid format. " + wl.serviceFormatText());

       dryrun = dryrun.toLowerCase();
       log.info("Creating task");
       task.init(bankedId, project, request, service, igoUser, dryrun); 
       log.info("Getting result");
       Future<Object> result = connQueue.submitTask(task);

       String returnCode = "";
       try{
         log.info("Waiting for result");
         returnCode =  (String)result.get();
         if(!returnCode.equals(Messages.SUCCESS) && !dryrun.equals("true")){
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

