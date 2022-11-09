package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.SetRequestStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;


@RestController
@RequestMapping("/")
public class SetStatuses {
    private final static Log log = LogFactory.getLog(SetStatuses.class);
    private final ConnectionPoolLIMS conn;

    public SetStatuses(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/setRequestStatuses")
    public List<String> getContent() {
        log.info("Starting /setRequestStatuses");
        SetRequestStatus task = new SetRequestStatus();
        Future<Object> result = conn.submitTask(task);
        List<String> values = new LinkedList<>();
        try {
            values = (List<String>) result.get();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            values.add("ERROR IN SETTING REQUEST STATUS: " + e.getMessage() + "TRACE: " + sw.toString());
        }
        return values;
    }
}