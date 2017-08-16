package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.List;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Set;
import java.util.Arrays;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import org.mskcc.limsrest.limsapi.*;
import org.mskcc.limsrest.connection.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@RestController
public class GetRecentDeliveries {

    private final ConnectionQueue connQueue; 
    private final GetDelivered task;
   private Log log = LogFactory.getLog(GetRecentDeliveries.class);

    public GetRecentDeliveries( ConnectionQueue connQueue, GetDelivered task){
        this.connQueue = connQueue;
        this.task = task;
    }



    @RequestMapping("/getRecentDeliveries")
    public List<RequestSummary> getContent(@RequestParam(value="time", defaultValue = "NULL") String time, @RequestParam(value="units", defaultValue="NULL") String units,
          @RequestParam(value="investigator", defaultValue = "NULL") String investigator) {
       List<RequestSummary> values = new LinkedList<>();
       log.info("Starting get recent deliveries since " + time );
       if(!time.equals("NULL") && !investigator.equals("NULL")){
         task.init(investigator, Integer.parseInt(time), units);
       } else if(!time.equals("NULL")){
        task.init(Integer.parseInt(time), units);
       } else if(!investigator.equals("NULL")) {
        task.init(investigator);
       } else{
        task.init();
       }
       Future<Object> result = connQueue.submitTask(task);
       try{
         values = (List<RequestSummary>)result.get();
       } catch(Exception e){
         values.add(new RequestSummary("ERROR: " + e.getMessage()));
       }
       return values;
    }

}

