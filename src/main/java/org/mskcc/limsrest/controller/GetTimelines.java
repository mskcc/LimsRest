package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.GetProjectHistory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Used on the alba IGO tools website, called about once a month.
 */
@RestController
@RequestMapping("/")
public class GetTimelines {
    private static Log log = LogFactory.getLog(GetTimelines.class);

    private final ConnectionPoolLIMS conn;

    public GetTimelines(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getTimeline")
    public LinkedList<GetProjectHistory.HistoricalEvent> getContent(@RequestParam(value = "project") String[] project) {
        LinkedList<GetProjectHistory.HistoricalEvent> timeline = new LinkedList<>();
        for (int i = 0; i < project.length; i++) {
            if (!Whitelists.requestMatches(project[i])) {
                return timeline;
            }
        }
        log.info("Starting get Timeline " + project[0]);
        GetProjectHistory task = new GetProjectHistory();
        task.init(project);
        Future<Object> result = conn.submitTask(task);
        try {
            timeline = new LinkedList((Set<GetProjectHistory.HistoricalEvent>) result.get());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        log.info("Completed timeline");
        return timeline;
    }
}