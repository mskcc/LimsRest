package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.DeliveryEmail;
import org.mskcc.limsrest.limsapi.GetDeliveryEmailDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@RestController
public class GetDeliveryEmail {
    private Log log = LogFactory.getLog(GetDeliveryEmail.class);
    private final ConnectionQueue connQueue;
    private final GetDeliveryEmailDetails task;

    public GetDeliveryEmail(ConnectionQueue connQueue, GetDeliveryEmailDetails getQc) {
        this.connQueue = connQueue;
        this.task = getQc;
    }

    @GetMapping("/getDeliveryEmail")
    public DeliveryEmail getContent(@RequestParam(value="request", required=true) String request) {
        log.info("Starting get /getDeliveryEmail for request: " + request);

        if (request == null)
            return null;

        task.init(request);
        Future<Object> result = connQueue.submitTask(task);
        try {
            return (DeliveryEmail)result.get();
        } catch(Exception e) {
            return new DeliveryEmail(e.getMessage());
        }
    }
}