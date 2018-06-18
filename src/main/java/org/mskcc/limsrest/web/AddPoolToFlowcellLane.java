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
public class AddPoolToFlowcellLane{ 


    private final ConnectionQueue connQueue; 
    private final AddPoolToLane task;
    private Log log = LogFactory.getLog(AddPoolToFlowcellLane.class);
   
    public AddPoolToFlowcellLane( ConnectionQueue connQueue, AddPoolToLane adder){
        this.connQueue = connQueue;
        this.task = adder;
    }



    @RequestMapping("/addPoolToFlowcellLane")
    public String getContent(@RequestParam(value="sample") String sample, @RequestParam(value="removeSample", defaultValue="NULL") String removeSample,
                           @RequestParam(value="flowcell") String flowcell, @RequestParam(value="igoUser") String igoUser,
                            @RequestParam(value="lane") String lane, @RequestParam(value="force", defaultValue="NULL") String force){
       log.info("Starting to add pool " + sample + " to flowcell lane " + lane  + "  by user " + igoUser);
        if(!Whitelists.sampleMatches(lane))
            return "FAILURE: lane is not using a valid format";
        if(!Whitelists.textMatches(flowcell))
            return "FAILURE: flowcell is not using a valid format";
        if(!Whitelists.sampleMatches(sample))
            return "FAILURE: sample is not using a valid format";
        if(!Whitelists.sampleMatches(removeSample))
            return "FAILURE: removeSample is not using a valid format";
        if(!Whitelists.textMatches(igoUser))
            return "FAILURE: igoUser is not using a valid format";

       boolean isForce = false;
       if(force.toUpperCase().equals("TRUE")){
            isForce = true;
       }
       task.init(flowcell, sample, removeSample, igoUser, Long.parseLong(lane), isForce);
       Future<Object> result = connQueue.submitTask(task);
       String returnCode = "";
       try{
         returnCode = "Record Id:" + (String)result.get();
       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          returnCode = "ERROR IN ADDING POOL TO LANE: " + e.getMessage() + " TRACE: " + sw.toString();
       }
       return returnCode;
    }

}

