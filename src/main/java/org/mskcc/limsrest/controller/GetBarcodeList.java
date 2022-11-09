package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.BarcodeSummary;
import org.mskcc.limsrest.service.GetBarcodeInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping("/")
public class GetBarcodeList {
    private static Log log = LogFactory.getLog(GetBarcodeList.class);
    private final ConnectionLIMS conn;

    public GetBarcodeList(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getBarcodeList")
    public List<BarcodeSummary> getContent(@RequestParam(value = "user", required=false) String user, HttpServletRequest request) {
        log.info("/getBarcodeList for user:" + user + ", client IP:" + request.getRemoteAddr());
        GetBarcodeInfo task = new GetBarcodeInfo(conn);
        return task.execute();
    }
}