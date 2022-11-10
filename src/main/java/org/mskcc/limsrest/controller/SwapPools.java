package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.AddSampleToPool;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class SwapPools {
    private final static Log log = LogFactory.getLog(SwapPools.class);
    private final ConnectionLIMS conn;


    public SwapPools(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/swapPools")
    public String getContent(@RequestParam(value = "sample") String sample, @RequestParam(value = "removePool", defaultValue = "NULL") String removePool, @RequestParam(value = "pool", defaultValue = "NULL") String pool, @RequestParam(value = "igoUser") String igoUser) {
        log.info("/swapPools Swapping sample " + sample + " from pool " + removePool + " to pool " + pool + " by user " + igoUser);

        AddSampleToPool task = new AddSampleToPool(pool, sample, removePool, igoUser, conn);
        return task.execute();
    }
}