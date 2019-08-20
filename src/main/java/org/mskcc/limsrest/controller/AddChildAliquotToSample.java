package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.AddChildSample;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Future;


@RestController
@RequestMapping("/")
public class AddChildAliquotToSample {
    private static Log log = LogFactory.getLog(AddChildAliquotToSample.class);
    private final ConnectionPoolLIMS conn;
    private final AddChildSample task = new AddChildSample();

    public AddChildAliquotToSample(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/addChildAliquotToSample")
    public String getContent(@RequestParam(value = "sample") String sample,
                             @RequestParam(value = "status") String status,
                             @RequestParam(value = "additionalType", defaultValue = "NULL") String additionalType,
                             @RequestParam(value = "igoUser") String igoUser,
                             @RequestParam(value = "childSample", defaultValue = "NULL") String childSample) {
        if (!Whitelists.sampleMatches(sample))
            return "FAILURE: sample is not using a valid format";
        log.info("Starting to add child aliquot to " + sample + " by" + igoUser);
        task.init(sample, status, additionalType, igoUser, childSample);
        Future<Object> result = conn.submitTask(task);
        try {
            return "Record Id:" + result.get();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return "ERROR IN ADDING POOL TO LANE: " + e.getMessage() + " TRACE: " + sw.toString();
        }
    }
}