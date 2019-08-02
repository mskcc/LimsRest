package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GetRequest;
import org.mskcc.limsrest.limsapi.LimsException;
import org.mskcc.limsrest.limsapi.RequestDetailed;
import org.mskcc.limsrest.limsapi.SetRequest;
import org.mskcc.limsrest.staticstrings.Messages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;


@RestController
public class LimsRequest {

    private final ConnectionQueue connQueue; 
    private final SetRequest task;
    private final GetRequest reader;    
    private Log log = LogFactory.getLog(LimsRequest.class);
   
    public LimsRequest( ConnectionQueue connQueue, SetRequest setter, GetRequest reader){
        this.connQueue = connQueue;
        this.task = setter;
        this.reader = reader;
    }

    @RequestMapping(value="/limsRequest",  method = RequestMethod.POST)
    public ResponseEntity<String> setContent(@RequestParam(value="request") String request, @RequestParam(value="igoUser") String igoUser, 
                           @RequestParam(value="user") String user, 
                           @RequestParam(value="readMe") String readMe){
       log.info("Starting a note with " + readMe + " on request " + request);
       if(!Whitelists.textMatches(igoUser))
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("igoUser is not using a valid format. " + Whitelists.textFormatText());
       if(!Whitelists.requestMatches(request))
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("request is not using a valid format. " + Whitelists.requestFormatText());


       HashMap<String, Object> requestFields = new HashMap<>();
       requestFields.put("ReadMe", readMe);
       task.init(igoUser, request, requestFields); 
                         
       Future<Object> result = connQueue.submitTask(task);
       String returnCode = "";
       try{
         returnCode =  (String)result.get();
         if(!returnCode.equals(Messages.SUCCESS)){
            throw new LimsException(returnCode);
         }
       } catch(Exception e){
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         returnCode =  e.getMessage() + "\nTRACE: " + sw.toString();
         log.info(returnCode);
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(returnCode);
                    
       }
       return ResponseEntity.ok(returnCode); 
    }

    @RequestMapping(value="/limsRequest",  method = RequestMethod.GET)
    public ResponseEntity<List<RequestDetailed>> setContent(@RequestParam(value="request") String[] request, @RequestParam(value="igoUser") String igoUser,
                           @RequestParam(value="user") String user,
                           @RequestParam(value="field", defaultValue = "NULL") String[] fieldName){
      LinkedList<RequestDetailed> reqSummary = new LinkedList<>();
      log.info("Getting a request " + request[0] + " for field" + fieldName[0]);
      for(String r : request){
          if(!Whitelists.requestMatches(r)){

           return new ResponseEntity(reqSummary, HttpStatus.BAD_REQUEST);
          }
      }
      for(String f : fieldName){
        if(!Whitelists.textMatches(igoUser))
           return new ResponseEntity(reqSummary, HttpStatus.BAD_REQUEST);
      }     
      reader.init(igoUser, request, fieldName);
      Future<Object> result = connQueue.submitTask(reader);
      try{
         reqSummary = (LinkedList<RequestDetailed>)result.get();
       } catch(Exception e){
            return new ResponseEntity(reqSummary, HttpStatus.BAD_REQUEST);
       }
       return ResponseEntity.ok((List<RequestDetailed>)reqSummary);
    }
}