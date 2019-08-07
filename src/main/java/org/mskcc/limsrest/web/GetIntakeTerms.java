package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.mskcc.limsrest.limsapi.*;
import org.mskcc.limsrest.connection.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@RestController
@RequestMapping("/")
public class GetIntakeTerms {
    private static Log log = LogFactory.getLog(GetIntakeTerms.class);
    private final ConnectionPoolLIMS conn;
    private final GetIntakeFormDescription task;

    public GetIntakeTerms(ConnectionPoolLIMS conn, GetIntakeFormDescription intake){
        this.conn = conn;
        this.task = intake;
    }

    @GetMapping("/getIntakeTerms")
    public List<List<String>> getContent(@RequestParam(value="type", defaultValue="NULL") String type, @RequestParam(value="recipe", defaultValue="NULL") String recipe) {
      List<List<String>> values = new LinkedList<>();
       if(!Whitelists.textMatches(type)){
          log.info( "FAILURE: type is not using a valid format");
           values.add(Arrays.asList("", "", "FAILURE: type is not using a valid format"));
           return values;
       } 
       if(!Whitelists.specialMatches(recipe)){
          log.info( "FAILURE: recipe is not using a valid format");
           values.add(Arrays.asList("", "", "FAILURE: recipe is not using a valid format"));
           return values;
       } 
        
       log.info("Starting get intake for " + type + " and " + recipe);
       type = type.replaceAll("_PIPI_SLASH_", "/");
       task.init(type, recipe);
       Future<Object> result = conn.submitTask(task);
       try{
         values = (List<List<String>>)result.get();
       } catch(Exception e){
         values.add(Arrays.asList("", "", e.getMessage()));
       }
       return values;
    }
}