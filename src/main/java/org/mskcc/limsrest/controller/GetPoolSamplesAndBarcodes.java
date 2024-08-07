package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetPoolsSamplesAndBarcodesTask;
import org.mskcc.limsrest.service.PoolInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
public class GetPoolSamplesAndBarcodes {
    private static Log log = LogFactory.getLog(GetPoolSamplesAndBarcodes.class);
    private final ConnectionLIMS conn;

    public GetPoolSamplesAndBarcodes(ConnectionLIMS conn) { this.conn = conn; }

    @GetMapping("/getPoolsBarcodes")
    public List<PoolInfo> getPoolsInfo(@RequestParam (value = "poolId") String poolId) {
        log.info("Starting /getPoolsBarcodes using pool ID " + poolId);
        GetPoolsSamplesAndBarcodesTask task = new GetPoolsSamplesAndBarcodesTask(conn, poolId);
        return task.execute();
    }
}
