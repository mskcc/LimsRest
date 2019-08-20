package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.FindBarcodeSequence;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetBarcodeSequence {
    private static Log log = LogFactory.getLog(GetBarcodeSequence.class);
    private final ConnectionPoolLIMS conn;
    private final FindBarcodeSequence task = new FindBarcodeSequence();

    public GetBarcodeSequence(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getBarcodeSequence")
    public String getContent(@RequestParam(value = "user") String user,
                             @RequestParam(value = "barcodeId") String barcodeId) {
        if (!Whitelists.textMatches(barcodeId))
            return "FAILURE: flowcell is not using a valid format";

        task.init(barcodeId);
        log.info("Starting get barcode sequence " + barcodeId);
        Future<Object> result = conn.submitTask(task);
        try {
            return (String) result.get();
        } catch (Exception e) {
            return "";
        }
    }
}