package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.GetDelivered;
import org.mskcc.limsrest.service.RequestSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Used by the QC site so they can view recently delivered projects in the QC meetings.
 * Also used by EmailDelivery code (https://github.com/mskcc/fastq-plus).
 */
@RestController
@RequestMapping("/")
public class GetRecentDeliveries {
    private static Log log = LogFactory.getLog(GetRecentDeliveries.class);
    private final ConnectionPoolLIMS conn;

    public GetRecentDeliveries(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getRecentDeliveries")
    public List<RequestSummary> getContent(@RequestParam(value = "time", defaultValue = "NULL") String time,
                                           @RequestParam(value = "units", defaultValue = "NULL") String units,
                                           @RequestParam(value = "investigator", defaultValue = "NULL") String investigator,
                                           HttpServletRequest request) {
        log.info("Starting /getRecentDeliveries?time=" + time + "&units=" + units + " client IP:" + request.getRemoteAddr());
        GetDelivered task = new GetDelivered();

        if (!time.equals("NULL") && !investigator.equals("NULL")) {
            // Request Projects: investigator & timeframe
            task.init(investigator, Integer.parseInt(time), units);
        } else if (!time.equals("NULL")) {
            // Request Projects: timeframe
            task.init(Integer.parseInt(time), units);
        } else if (!investigator.equals("NULL")) {
            // Request Projects: investigator (timeframe defaulted to within 2 weeks)
            task.init(investigator);
        } else {
            // Sequencing Projects, i.e. results in the SeqAnalysisSampleQC w/ a non-Passed/Failed status
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