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
public class SetStatuses {

    private final ConnectionQueue connQueue; 
    private final SetRequestStatus task;
    private Log log = LogFactory.getLog(SetStatuses.class);
   
    public SetStatuses( ConnectionQueue connQueue, SetRequestStatus requestStatus){
        this.connQueue = connQueue;
        this.task = requestStatus;
    }



    @RequestMapping("/setRequestStatuses")
    public List<String> getContent() {
       log.info("Starting to set request statuses");
       Future<Object> result = connQueue.submitTask(task);
       List<String> values = new LinkedList<>(); 
       try{
         values = (List<String>)result.get();
       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
         values.add("ERROR IN SETTING REQUEST STATUS: " + e.getMessage() + "TRACE: " + sw.toString());
       }
       return values;
    }

}

