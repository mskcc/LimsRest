package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.SetRequestStatus;
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
    private final ConnectionQueue connQueue;
    private final SetRequestStatus task;

    public SetStatuses(ConnectionQueue connQueue, SetRequestStatus requestStatus) {
        this.connQueue = connQueue;
        this.task = requestStatus;
    }

    @GetMapping("/setRequestStatuses")
    public List<String> getContent() {
        log.info("Starting to set request statuses");
        Future<Object> result = connQueue.submitTask(task);
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