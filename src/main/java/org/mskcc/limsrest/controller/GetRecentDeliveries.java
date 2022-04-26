package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetDeliveredTask;
import org.mskcc.limsrest.service.RequestSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;

/**
 * Used by the QC site so they can view recently delivered projects in the QC meetings.
 */
@RestController
@RequestMapping("/")
public class GetRecentDeliveries {
    private static Log log = LogFactory.getLog(GetRecentDeliveries.class);
    private final ConnectionLIMS conn;

    public GetRecentDeliveries(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getRecentDeliveries")
    public List<RequestSummary> getContent(@RequestParam(value = "time", defaultValue = "NULL") String time,
                                           @RequestParam(value = "units", defaultValue = "NULL") String units,
                                           @RequestParam(value = "investigator", defaultValue = "NULL") String investigator,
                                           HttpServletRequest request) {
        log.info("Starting /getRecentDeliveries?time=" + time + "&units=" + units + " client IP:" + request.getRemoteAddr());
        GetDeliveredTask task = new GetDeliveredTask(conn);

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

        try {
            long start = System.currentTimeMillis();
            List<RequestSummary> result = (List<RequestSummary>) task.execute();
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            log.info("Elapsed time for running GetDelivered (ms): " + timeElapsed);
            return result;
        } catch (Exception e) {
            List<RequestSummary> values = new LinkedList<>();
            values.add(new RequestSummary("ERROR: " + e.getMessage()));
            return values;
        }
    }
}