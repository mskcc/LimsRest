package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionPoolLIMS;
import org.mskcc.limsrest.limsapi.GetDelivered;
import org.mskcc.limsrest.limsapi.RequestSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Used by the QC site so they can view recently delivered projects in the QC meetings.
 */
@RestController
@RequestMapping("/")
public class GetRecentDeliveries {
    private static Log log = LogFactory.getLog(GetRecentDeliveries.class);
    private final ConnectionPoolLIMS conn;
    private final GetDelivered task = new GetDelivered();

    public GetRecentDeliveries(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getRecentDeliveries")
    public List<RequestSummary> getContent(@RequestParam(value = "time", defaultValue = "NULL") String time,
                                           @RequestParam(value = "units", defaultValue = "NULL") String units,
                                           @RequestParam(value = "investigator", defaultValue = "NULL") String investigator) {
        log.info("Starting get recent deliveries since " + time);

        if (!time.equals("NULL") && !investigator.equals("NULL")) {
            task.init(investigator, Integer.parseInt(time), units);
        } else if (!time.equals("NULL")) {
            task.init(Integer.parseInt(time), units);
        } else if (!investigator.equals("NULL")) {
            task.init(investigator);
        } else {
            task.init();
        }

        Future<Object> result = conn.submitTask(task);
        try {
            return (List<RequestSummary>) result.get();
        } catch (Exception e) {
            List<RequestSummary> values = new LinkedList<>();
            values.add(new RequestSummary("ERROR: " + e.getMessage()));
            return values;
        }
    }
}