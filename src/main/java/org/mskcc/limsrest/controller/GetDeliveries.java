package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.GetDelivered;
import org.mskcc.limsrest.service.GetDeliveriesTask;
import org.mskcc.limsrest.service.RequestSummary;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Intended for pipeline kickoff customers to query recent deliveries they can process begin processing.
 */
@RestController
@RequestMapping("/")
public class GetDeliveries {
    private static Log log = LogFactory.getLog(GetDeliveries.class);
    private final ConnectionPoolLIMS conn;

    public GetDeliveries(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/api/getDeliveries")
    public List<GetDeliveriesTask.Delivery> getContent(@RequestParam(value = "timestamp") long timestamp,
                                                       HttpServletRequest request) {
        log.info("Starting /getDeliveries?timestamp=" + timestamp + " client IP:" + request.getRemoteAddr());

        if (timestamp < 0 || timestamp > System.currentTimeMillis()) {
            log.error("FAILURE: invalid timestamp - " + timestamp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid timestamp");
        }

        GetDeliveriesTask task = new GetDeliveriesTask();
        task.init(timestamp);

        Future<Object> result = conn.submitTask(task);
        try {
            return (List<GetDeliveriesTask.Delivery>) result.get();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}