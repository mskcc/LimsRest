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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@RestController
public class GetPickListValues {

    private final ConnectionQueue connQueue; 
    private final GetPickList task;
//    private static final Logger logger = LoggerFactory.getLogger(GetProjectQc.class);
   private Log log = LogFactory.getLog(GetPickListValues.class);

    public GetPickListValues( ConnectionQueue connQueue, GetPickList picklist){
        this.connQueue = connQueue;
        this.task = picklist;
    }



    @RequestMapping("/getPickListValues")
    public List<String> getContent(@RequestParam(value="list", defaultValue="Countries") String list) {
       List<String> values = new LinkedList<>();
        Whitelists wl = new Whitelists();
       if(!wl.textMatches(list)){
          log.info( "FAILURE: list is not using a valid format");
           values.add("FAILURE: list is not using a valid format");
           return values;
       } 
       task.init(list);
       log.info("Starting picklist query for " + list);
       Future<Object> result = connQueue.submitTask(task);
       try{
         values = (List<String>)result.get();
       } catch(Exception e){
         values.add("ERROR: " + e.getMessage());
       }
       return values;
    }

}

