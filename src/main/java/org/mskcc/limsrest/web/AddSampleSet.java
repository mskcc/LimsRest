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
    public String getContent( @RequestParam(value="igoId", required=false) String[] igoId,
                               @RequestParam(value="user") String user, @RequestParam(value="pair", required=false) String[] pair,  @RequestParam(value="category", required=false) String[] category,
                               @RequestParam(value="project", required=false) String[] request, @RequestParam(value="igoUser") String igoUser,
                               @RequestParam(value="setName", required=false) String name, @RequestParam(value="mapName", required=false) String mapName, 
                               @RequestParam(value="baitSet", required=false) String baitSet, @RequestParam(value="primeRecipe", required=false) String primeRecipe,
                                @RequestParam(value="primeRequest", required=false) String primeRequest, @RequestParam(value="externalSpecimen", required=false) String[] externalSpecimen) {
       log.info("Adding to sample set " + name + " at a request by user " + user);
        if(igoId != null){
            for(int i = 0; i < igoId.length; i++){
                if(!Whitelists.sampleMatches(igoId[i]))
                    return "FAILURE: igoId is not using a valid format";
            }
        }
       if(!Whitelists.textMatches(igoUser))
            return "FAILURE: igoUser is not using a valid format";
        
        if(!Whitelists.textMatches(name)){
            return "FAILURE: setName is not using a valid format";
        }
        if(request != null){
            for(int i = 0; i < request.length; i++){
                if(!Whitelists.requestMatches(request[i]))
                    return "FAILURE: project is not using a valid format";
            }
        }
        if(!Whitelists.textMatches(baitSet)){
           return "FAILURE: baitSet is not using a valid format";
        }
        if(!Whitelists.textMatches(primeRecipe)){
           return "FAILURE: primeRecipe is not using a valid format";
        }

       if(externalSpecimen != null){
          for(String spec : externalSpecimen){
             if(!Whitelists.textMatches(spec)){
                return "FAILURE: externalSpecimen is not using a valid format";
             }
          }
      }
      task.init(igoUser, name, mapName, request, igoId, pair, category, baitSet, primeRecipe, primeRequest, externalSpecimen);       
       Future<Object> result = connQueue.submitTask(task);
       String returnCode = "";
       try{
         returnCode = "Record Id:" + (String)result.get();

       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          returnCode = "ERROR IN ADDING TO SAMPLE SET: " + e.getMessage(); 
       }
       return returnCode;
    }

}

