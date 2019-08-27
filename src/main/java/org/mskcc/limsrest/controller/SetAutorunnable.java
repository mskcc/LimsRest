package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.ToggleAutorunnable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/")
public class SetAutorunnable {
    private final static Log log = LogFactory.getLog(SetAutorunnable.class);
    private final ConnectionPoolLIMS conn;

    public SetAutorunnable(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/setAllAutorunnable")
    public List<String> getContent() {
        ToggleAutorunnable task = new ToggleAutorunnable();
        task.init("ALL", "true", null, null);
        log.info("Setting all autorunnable");
        Future<Object> result = conn.submitTask(task);
        List<String> values = new LinkedList<>();
        try {
            values = (List<String>) result.get();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            values.add("ERROR IN SETTING AUTORUN STATUS: " + e.getMessage() + "TRACE: " + sw.toString());
        }
        return values;
    }

    @GetMapping("/setAutorunnable")
    public List<String> setAutorunnable(@RequestParam(value = "request") String req,
                                        @RequestParam(value = "status") String status,
                                        @RequestParam(value = "comment", required = false) String comment,
                                        @RequestParam(value = "igoUser", required = false) String igoUser) {
        ToggleAutorunnable task = new ToggleAutorunnable();
        log.info("Setting autorunable for " + req + " to value " + status + " with comment " + comment);
        Pattern requestPattern = Pattern.compile("[0-9]{5,7}(_[A-Z]+)?");
        Matcher matcher = requestPattern.matcher(req);
        List<String> values = new LinkedList<>();
        if (req == null || !matcher.matches() || status == null || !(status.toLowerCase().equals("true") || status.toLowerCase().equals("false"))) {
            values.add("ERROR: Must define request and status and must be a valid request id and status may only be true or false");
            return values;
        }
        if (!Whitelists.textMatches(igoUser)) {
            values.add("FAILURE: igoUser is not using a valid format");
            return values;
        }
        task.init(req, status, comment, igoUser);
        Future<Object> result = conn.submitTask(task);
        try {
            values = (List<String>) result.get();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            values.add("ERROR IN SETTING REQUEST STATUS: " + e.getMessage() + " TRACE: " + sw.toString());
        }
        return values;
    }
}