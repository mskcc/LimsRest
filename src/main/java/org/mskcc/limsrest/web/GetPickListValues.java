package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GetPickList;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

@RestController
public class GetPickListValues {
    private static final Log log = LogFactory.getLog(GetPickListValues.class);

    private final ConnectionQueue connQueue;
    private final GetPickList task;

    public GetPickListValues( ConnectionQueue connQueue, GetPickList picklist){
        this.connQueue = connQueue;
        this.task = picklist;
    }

    @RequestMapping("/getPickListValues")
    public List<String> getContent(
            @RequestParam(value = "list", defaultValue = "Countries") String list,
            @RequestParam(value = "notify", required = false) boolean notifyOfChanges) {
       List<String> values = new LinkedList<>();
       if(!Whitelists.textMatches(list)){
          log.info( "FAILURE: list is not using a valid format");
           values.add("FAILURE: list is not using a valid format");
           return values;
       }
        task.init(list, notifyOfChanges);
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