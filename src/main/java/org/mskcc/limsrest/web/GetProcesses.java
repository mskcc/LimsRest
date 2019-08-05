package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GetProcessNames;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetProcesses {
    private static Log log = LogFactory.getLog(GetProcesses.class);
    private final ConnectionQueue connQueue;
    private final GetProcessNames task;

    public GetProcesses(ConnectionQueue connQueue, GetProcessNames processNamer) {
        this.connQueue = connQueue;
        this.task = processNamer;
    }

    @GetMapping("/getProcesses")
    public List<String> getContent(@RequestParam(value = "user") String user) {
        log.info("Starting process name query for user " + user);
        Future<Object> result = connQueue.submitTask(task);
        List<String> values = new LinkedList<>();
        try {
            values = (List<String>) result.get();
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
        return values;
    }
}