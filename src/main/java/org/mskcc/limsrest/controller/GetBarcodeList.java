package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.BarcodeSummary;
import org.mskcc.limsrest.service.GetBarcodeInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class GetBarcodeList {
    private static Log log = LogFactory.getLog(GetBarcodeList.class);
    private final ConnectionPoolLIMS conn;

    public GetBarcodeList(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getBarcodeList")
    public List<BarcodeSummary> getContent(@RequestParam(value = "user", required=false) String user, HttpServletRequest request) {
        log.info("/getBarcodeList for user:" + user + ", client IP:" + request.getRemoteAddr());
        GetBarcodeInfo task = new GetBarcodeInfo();
        Future<Object> result = conn.submitTask(task);
        try {
            return (List<BarcodeSummary>) result.get();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new LinkedList();
    }
}