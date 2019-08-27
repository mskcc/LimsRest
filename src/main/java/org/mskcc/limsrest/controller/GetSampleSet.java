package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.GetSet;
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
    private final ConnectionPoolLIMS conn;

    public GetSampleSet(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getSampleSet")
    public List<String> getContent(@RequestParam(value = "setName") String name, @RequestParam(value = "user") String user) {
        log.info("Starting to get sample set " + name + " for user " + user);
        List<String> sets = new LinkedList<>();
        if (!Whitelists.requestMatches(name)) {
            sets.add("FAILURE: setName is not using a valid format");
            return sets;
        }
        GetSet task = new GetSet();
        task.init(name);
        Future<Object> result = conn.submitTask(task);
        try {
            sets = (List<String>) result.get();
        } catch (Exception e) {
            sets.add(e.getMessage());
        }
        return sets;
    }
}