package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetDeliveriesTask;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Intended for pipeline kickoff customers to query recent deliveries they can process begin processing.
 */
@RestController
@RequestMapping("/")
public class GetDeliveries {
    private static Log log = LogFactory.getLog(GetDeliveries.class);
    private ConnectionLIMS conn;

    public GetDeliveries(ConnectionLIMS conn) {
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

        GetDeliveriesTask task = new GetDeliveriesTask(timestamp, conn);

        return (List<GetDeliveriesTask.Delivery>) task.execute();
    }
}