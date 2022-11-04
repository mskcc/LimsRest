package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.GetExemplarConfigTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.mskcc.limsrest.model.ExemplarConfig;

import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping("/")
public class GetExemplarConfiguration {
    private static Log log = LogFactory.getLog(GetExemplarConfiguration.class);
    private final ConnectionLIMS conn;

    public GetExemplarConfiguration(ConnectionLIMS conn){
        this.conn = conn;
    }

    @GetMapping("/getConfig")
    public ExemplarConfig getConfig() {

        log.info("Starting /getConfig");

        GetExemplarConfigTask exemplarConfigTask = new GetExemplarConfigTask(conn);
        ExemplarConfig configData = new ExemplarConfig();
        try {
            configData = exemplarConfigTask.execute();
        }
        catch(Exception e) {
            log.info(String.format("While getting exemplar configuration data an exception is thrown: %s", e.getMessage()));
        }
        return configData;

    }

}