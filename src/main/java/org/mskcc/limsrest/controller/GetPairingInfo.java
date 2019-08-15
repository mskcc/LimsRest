package org.mskcc.limsrest.controller;

import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.concurrent.Future;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.mskcc.limsrest.service.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


@RestController
@RequestMapping("/") @Deprecated // no longer used/moved to pipeline-kickoff
public class GetPairingInfo {

    private final ConnectionPoolLIMS conn;
    private final GetSetOrReqPairs task;
    private final Log log = LogFactory.getLog(GetPairingInfo.class);

    public GetPairingInfo(ConnectionPoolLIMS conn, GetSetOrReqPairs getPairs){
        this.conn = conn;
        this.task = getPairs;
    }

    @GetMapping("/getCategoryMapping")
       public List<HashMap<String, String>> getMappingContent(@RequestParam(value="project", required=false) String requestId, @RequestParam(value="setName", required=false) String set, @RequestParam(value="mapName", required = false) String mapName, @RequestParam(value="user") String user){
       List<HashMap<String, String>> typeToId = new LinkedList<>();
       if(!Whitelists.sampleMatches(set)){
            HashMap<String, String> result = new HashMap<>();
            result.put("ERROR", "setName is not using a valid format");
            typeToId.add(result);
            return typeToId;
        }
       if(!Whitelists.requestMatches(requestId)){
            HashMap<String, String> result = new HashMap<>();
            result.put("ERROR", "project is not using a valid format");
            typeToId.add(result);
            return typeToId; 
        }
       log.info("Starting to get pairing info for user " + user);
       task.init(requestId, set, mapName);
       if(requestId == null && set == null){
          log.info("Trying to access catergory mapping without specifying project or set");
          return typeToId;
       }
       Future<Object> result = conn.submitTask(task);
       try{
           typeToId = (List<HashMap<String, String>>)result.get();
        } catch(Exception e){
             StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw);
             e.printStackTrace(pw);
             log.info(e.getMessage() + " TRACE: " + sw.toString());
        }
        return typeToId;
       }

    @GetMapping("/getPairingInfo")
        public List<HashMap<String, String>> getContent(@RequestParam(value="project", required=false) String requestId, @RequestParam(value="setName", required=false) String set,@RequestParam(value="user") String user) {
            List<HashMap<String, String>> typeToId = new LinkedList<>();
            if(!Whitelists.sampleMatches(set)){
                HashMap<String, String> result = new HashMap<>();
                result.put("ERROR", "setName is not using a valid format");
                typeToId.add(result);
                return typeToId;
            }
            if(!Whitelists.requestMatches(requestId)){
                HashMap<String, String> result = new HashMap<>();
                result.put("ERROR", "project is not using a valid format");
                typeToId.add(result);
                return typeToId;
            }
            log.info("Starting to get pairing info for user " + user);
            task.init(requestId, set);
            if(requestId == null && set == null){
               log.info("Trying to access pairing info without specifying project or set");
               return typeToId;
            }
            Future<Object> result = conn.submitTask(task);
            try{
                typeToId = (List<HashMap<String, String>>)result.get();
            } catch(Exception e){
             StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw);
             e.printStackTrace(pw);
             log.info(e.getMessage() + " TRACE: " + sw.toString());                
            
            }
            return typeToId;
        }
}