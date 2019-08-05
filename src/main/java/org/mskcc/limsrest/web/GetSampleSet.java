package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GetSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetSampleSet {
    private static Log log = LogFactory.getLog(GetSampleSet.class);
    private final ConnectionQueue connQueue;
    private final GetSet task;

    public GetSampleSet(ConnectionQueue connQueue, GetSet getSet) {
        this.connQueue = connQueue;
        this.task = getSet;
    }

    @GetMapping("/getSampleSet")
    public List<String> getContent(@RequestParam(value = "setName") String name, @RequestParam(value = "user") String user) {
        log.info("Starting to get sample set " + name + " for user " + user);
        List<String> sets = new LinkedList<>();
        if (!Whitelists.requestMatches(name)) {
            sets.add("FAILURE: setName is not using a valid format");
            return sets;
        }
        task.init(name);
        Future<Object> result = connQueue.submitTask(task);
        try {
            sets = (List<String>) result.get();
        } catch (Exception e) {
            sets.add(e.getMessage());
        }
        return sets;
    }
}