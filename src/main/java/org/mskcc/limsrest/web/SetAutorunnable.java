package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class SetAutorunnable {

    private final ConnectionQueue connQueue; 
    private final ToggleAutorunnable task;
     private Log log = LogFactory.getLog(SetAutorunnable.class);

    public SetAutorunnable( ConnectionQueue connQueue, ToggleAutorunnable toggle){
        this.connQueue = connQueue;
        this.task = toggle;
    }



    @RequestMapping("/setAllAutorunnable")
    public List<String> getContent() {
       task.init("ALL", "true", null, null);
       log.info("Setting all autorunnable");
       Future<Object> result = connQueue.submitTask(task);
       List<String> values = new LinkedList<>(); 
       try{
         values = (List<String>)result.get();
       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
         values.add("ERROR IN SETTING AUTORUN STATUS: " + e.getMessage() + "TRACE: " + sw.toString());
       }
       return values;
    }

    @RequestMapping("/setAutorunnable")
    public List<String> setAutorunnable(@RequestParam(value="request") String req, @RequestParam(value="status") String status, @RequestParam(value="comment", required=false) String comment, @RequestParam(value="igoUser", required=false) String igoUser){
     log.info("Setting autorunable for " + req + " to value " + status + " with comment " + comment);
     Pattern requestPattern = Pattern.compile("[0-9]{5,7}(_[A-Z]+)?");
     Matcher matcher = requestPattern.matcher(req);
     List<String> values = new LinkedList<>();
     Whitelists wl = new Whitelists();
     if(req == null || !matcher.matches() || status == null || !(status.toLowerCase().equals("true") || status.toLowerCase().equals("false"))){
        values.add("ERROR: Must define request and status and must be a valid request id and status may only be true or false");
        return values;
     }
     if(!wl.textMatches(igoUser)){
        values.add("FAILURE: igoUser is not using a valid format");
        return values;
     }
      task.init(req, status, comment, igoUser);
      Future<Object> result = connQueue.submitTask(task);
      try{
         values = (List<String>)result.get();
       } catch(Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
         values.add("ERROR IN SETTING REQUEST STATUS: " + e.getMessage() + " TRACE: " + sw.toString());
       }
       return values;
   }
}
