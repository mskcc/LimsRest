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
public class SwapPools{ 


    private final ConnectionQueue connQueue; 
    private final AddSampleToPool task;
    private Log log = LogFactory.getLog(SwapPools.class);
   
    public SwapPools( ConnectionQueue connQueue, AddSampleToPool adder){
        this.connQueue = connQueue;
        this.task = adder;
    }
    
    @RequestMapping("/swapPools")
    public String getContent(@RequestParam(value="sample") String sample, @RequestParam(value="removePool", defaultValue="NULL") String removePool, @RequestParam(value="pool", defaultValue="NULL") String pool,  @RequestParam(value="igoUser") String igoUser){
       log.info("Swapping sample " + sample + " from pool " + removePool + " to pool " + pool + " by user " + igoUser);
       
       task.init(pool, sample, removePool, igoUser);
       Future<Object> result = connQueue.submitTask(task);
       String returnCode = "";
       try{
         returnCode = "Record Id:" + (String)result.get();
       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          returnCode = "ERROR IN SWAPPING POOL: " + e.getMessage() + " TRACE: " + sw.toString();
       }
       return returnCode;
    }

}

