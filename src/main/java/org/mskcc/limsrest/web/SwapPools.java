package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.AddSampleToPool;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@RestController
public class SwapPools {
    private final Log log = LogFactory.getLog(SwapPools.class);
    private final ConnectionQueue connQueue;
    private final AddSampleToPool task;

    public SwapPools(ConnectionQueue connQueue, AddSampleToPool adder) {
        this.connQueue = connQueue;
        this.task = adder;
    }

    @GetMapping("/swapPools")
    public String getContent(@RequestParam(value = "sample") String sample, @RequestParam(value = "removePool", defaultValue = "NULL") String removePool, @RequestParam(value = "pool", defaultValue = "NULL") String pool, @RequestParam(value = "igoUser") String igoUser) {
        log.info("Swapping sample " + sample + " from pool " + removePool + " to pool " + pool + " by user " + igoUser);

        task.init(pool, sample, removePool, igoUser);
        Future<Object> result = connQueue.submitTask(task);
        try {
            return "Record Id:" + (String) result.get();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "ERROR IN SWAPPING POOL: " + e.getMessage();
        }
    }
}