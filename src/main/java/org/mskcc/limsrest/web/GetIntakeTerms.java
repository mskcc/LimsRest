package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.List;
import java.util.LinkedList;
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
public class GetIntakeTerms {

    private final ConnectionQueue connQueue; 
    private final GetIntakeFormDescription task;
   private Log log = LogFactory.getLog(GetIntakeTerms.class);

    public GetIntakeTerms( ConnectionQueue connQueue, GetIntakeFormDescription intake){
        this.connQueue = connQueue;
        this.task = intake;
    }



    @RequestMapping("/getIntakeTerms")
    public List<List<String>> getContent(@RequestParam(value="type", defaultValue="NULL") String type, @RequestParam(value="recipe", defaultValue="NULL") String recipe) {
      List<List<String>> values = new LinkedList<>();
       Whitelists wl = new Whitelists();
       if(!wl.textMatches(type)){
          log.info( "FAILURE: type is not using a valid format");
           values.add(Arrays.asList("", "", "FAILURE: type is not using a valid format"));
           return values;
       } 
       if(!wl.specialMatches(recipe)){
          log.info( "FAILURE: recipe is not using a valid format");
           values.add(Arrays.asList("", "", "FAILURE: recipe is not using a valid format"));
           return values;
       } 
        
       log.info("Starting get intake for " + type + " and " + recipe);
       type = type.replaceAll("_PIPI_SLASH_", "/");
       task.init(type, recipe);
       Future<Object> result = connQueue.submitTask(task);
       try{
         values = (List<List<String>>)result.get();
       } catch(Exception e){
         values.add(Arrays.asList("", "", e.getMessage()));
       }
       return values;
    }

}

