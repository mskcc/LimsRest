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
public class AddSampleSet {

    private final ConnectionQueue connQueue; 
    private final AddOrCreateSet task;
    private Log log = LogFactory.getLog(AddSampleSet.class);
   
    public AddSampleSet( ConnectionQueue connQueue, AddOrCreateSet requestStatus){
        this.connQueue = connQueue;
        this.task = requestStatus;
    }


    @RequestMapping("/addSampleSet")
    public String getContent(@RequestParam(value="sample", required=false) String[] sample, @RequestParam(value="igoId", required=false) String[] igoId,
                               @RequestParam(value="user") String user, @RequestParam(value="pair", required=false) String[] pair,  @RequestParam(value="category", required=false) String[] category,
                               @RequestParam(value="project", required=false) String[] request, @RequestParam(value="igoUser") String igoUser, @RequestParam(value="validate", defaultValue="false") String validate,
                               @RequestParam(value="setName", required=false) String name, @RequestParam(value="mapName", required=false) String mapName) {
       log.info("Adding to sample set " + name + " at a request by user " + user);
       Whitelists wl = new Whitelists();
        if(sample != null){ //samples here are not standard format, as they are of form REQUESTID:SAMPLEID
           for(int i = 0; i < sample.length; i++){
                if(!wl.textMatches(sample[i]))
                    return "FAILURE: sample is not using a valid format";
            }
        }
        if(igoId != null){
            for(int i = 0; i < igoId.length; i++){
                if(!wl.sampleMatches(igoId[i]))
                    return "FAILURE: igoId is not using a valid format";
            }
        }
       if(!wl.textMatches(igoUser))
            return "FAILURE: igoUser is not using a valid format";
        
        if(!wl.textMatches(name)){
            return "FAILURE: setName is not using a valid format";
        }
        if(request != null){
            for(int i = 0; i < request.length; i++){
                if(!wl.requestMatches(request[i]))
                    return "FAILURE: project is not using a valid format";
            }
        }

       boolean val  = false;
       if(validate.equalsIgnoreCase("true")){
          val = true;
       }
      task.init(igoUser, name, mapName, request, sample, igoId, pair, category, true);       
       Future<Object> result = connQueue.submitTask(task);
       String returnCode = "";
       try{
         returnCode = "Record Id:" + (String)result.get();

       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          returnCode = "ERROR IN ADDING TO SAMPLE SET: " + e.getMessage() + " TRACE: " + sw.toString(); 
       }
       return returnCode;
    }

}

