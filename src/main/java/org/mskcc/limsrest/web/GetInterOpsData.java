package org.mskcc.limsrest.web;

import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GetSamples;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GetInterOpsData {
    private final ConnectionQueue connQueue;
    private final GetInterOpsDataTask task;

    public GetInterOpsData(ConnectionQueue connQueue, GetInterOpsDataTask task) {
        this.connQueue = connQueue;
        this.task = task;
    }

    @RequestMapping("/getInterOpsData")
    public List<String> getInterOps(@RequestParam String runId) {

    }
}
