package org.mskcc.limsrest.controller;

import java.util.concurrent.Future;
import java.util.List;
import java.util.LinkedList;

import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.mskcc.limsrest.service.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@RestController
@RequestMapping("/")
public class GetPickListValues {
    private static Log log = LogFactory.getLog(GetPickListValues.class);
    private final ConnectionPoolLIMS conn;

    private final GetPickList task = new GetPickList();

    public GetPickListValues(ConnectionPoolLIMS conn){
        this.conn = conn;
    }

    @GetMapping("/getPickListValues")
    public List<String> getContent(@RequestParam(value = "list", defaultValue = "Countries") String list) {
        List<String> values = new LinkedList<>();
        if (!Whitelists.textMatches(list)) {
            log.info("FAILURE: list is not using a valid format");
            values.add("FAILURE: list is not using a valid format");
            return values;
        }
        task.init(list);
        log.info("Starting /getPickListValues query for " + list);
        Future<Object> result = conn.submitTask(task);
        try {
            values = (List<String>) result.get();
        } catch (Exception e) {
            values.add("ERROR: " + e.getMessage());
        }
        return values;
    }
}