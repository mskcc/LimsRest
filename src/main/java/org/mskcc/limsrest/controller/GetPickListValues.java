package org.mskcc.limsrest.controller;

import java.util.List;
import java.util.LinkedList;

import org.mskcc.limsrest.ConnectionLIMS;
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
    private final ConnectionLIMS conn;

    public GetPickListValues(ConnectionLIMS conn){
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
        GetPickListTask task = new GetPickListTask(list, conn);
        log.info("Starting /getPickListValues query for " + list);
        List<String> result = task.execute();
        return result;
    }
}