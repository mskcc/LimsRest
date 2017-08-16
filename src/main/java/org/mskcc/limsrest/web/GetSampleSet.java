package org.mskcc.limsrest.web;

import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.Future;

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
public class GetSampleSet {

    private final ConnectionQueue connQueue; 
    private final GetSet task;
    private Log log = LogFactory.getLog(GetSampleSet.class);

    public GetSampleSet( ConnectionQueue connQueue, GetSet getSet){
        this.connQueue = connQueue;
        this.task = getSet;
    }



    @RequestMapping("/getSampleSet")
        public List<String> getContent(@RequestParam(value="setName") String name, @RequestParam(value="user") String user) {
            log.info("Starting to get sample set " + name + " for user " + user);
            List<String> sets = new LinkedList<>();
            Whitelists wl = new Whitelists();
            if(!wl.requestMatches(name)){
                sets.add( "FAILURE: setName is not using a valid format");
                return sets;
            }
            task.init(name);
            Future<Object> result = connQueue.submitTask(task);
            try{
                sets = (List<String>)result.get();
            } catch(Exception e){
                sets.add(e.getMessage());
            }
            return sets;
        }


}

