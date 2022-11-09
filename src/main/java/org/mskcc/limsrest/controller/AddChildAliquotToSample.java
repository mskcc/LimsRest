package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.AddChildSample;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/")
public class AddChildAliquotToSample {
    private static Log log = LogFactory.getLog(AddChildAliquotToSample.class);
    private final ConnectionLIMS conn;

    public AddChildAliquotToSample(ConnectionLIMS conn) {
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
        log.info("Starting /addChildAliquotToSample " + sample + " by" + igoUser);
        AddChildSample task = new AddChildSample(sample, status, additionalType, igoUser, childSample, conn);
        return task.execute();
    }
}