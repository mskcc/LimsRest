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
public class AddChildAliquotToSample{ 


    private final ConnectionQueue connQueue; 
    private final AddChildSample task;
    private Log log = LogFactory.getLog(AddChildAliquotToSample.class);
   
    public AddChildAliquotToSample(ConnectionQueue connQueue, AddChildSample adder){
        this.connQueue = connQueue;
        this.task = adder;
    }



    @RequestMapping("/addChildAliquotToSample")
    public String getContent(@RequestParam(value="sample") String sample, 
                           @RequestParam(value="status") String status, 
                           @RequestParam(value="additionalType", defaultValue="NULL") String additionalType,
                           @RequestParam(value="igoUser") String igoUser,
                           @RequestParam(value="childSample", defaultValue="NULL") String childSample){
       Whitelists wl = new Whitelists();
       if(!wl.sampleMatches(sample))
            return "FAILURE: sample is not using a valid format";
       log.info("Starting to add child aliquot to " + sample + " by"  + igoUser);
       task.init( sample, status, additionalType, igoUser, childSample);
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

