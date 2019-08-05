package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.BarcodeSummary;
import org.mskcc.limsrest.limsapi.GetBarcodeInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetBarcodeList {
    private static Log log = LogFactory.getLog(GetBarcodeList.class);
    private final ConnectionQueue connQueue;
    private final GetBarcodeInfo task;

    public GetBarcodeList(ConnectionQueue connQueue, GetBarcodeInfo barcodes) {
        this.connQueue = connQueue;
        this.task = barcodes;
    }

    @GetMapping("/getBarcodeList")
    public List<BarcodeSummary> getContent(@RequestParam(value = "user", required=false) String user) {
        log.info("Starting get barcode list for user" + user);
        Future<Object> result = connQueue.submitTask(task);
        try {
            return (List<BarcodeSummary>) result.get();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new LinkedList();
    }
}