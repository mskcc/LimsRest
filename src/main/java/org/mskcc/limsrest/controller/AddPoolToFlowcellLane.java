package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.AddPoolToLane;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class AddPoolToFlowcellLane{
    private static Log log = LogFactory.getLog(AddPoolToFlowcellLane.class);
    private final ConnectionLIMS conn;
   
    public AddPoolToFlowcellLane( ConnectionLIMS conn){
        this.conn = conn;
    }

    @GetMapping("/addPoolToFlowcellLane")
    public String getContent(@RequestParam(value="sample") String sample, @RequestParam(value="removeSample", defaultValue="NULL") String removeSample,
                           @RequestParam(value="flowcell") String flowcell, @RequestParam(value="igoUser") String igoUser,
                            @RequestParam(value="lane") String lane, @RequestParam(value="force", defaultValue="NULL") String force){
        log.info("Starting /addPoolToFlowcellLane to add pool " + sample + " to flowcell lane " + lane  + "  by user " + igoUser);
        if(!Whitelists.sampleMatches(lane))
            return "FAILURE: lane is not using a valid format";
        if(!Whitelists.textMatches(flowcell))
            return "FAILURE: flowcell is not using a valid format";
        if(!Whitelists.sampleMatches(sample))
            return "FAILURE: sample is not using a valid format";
        if(!Whitelists.sampleMatches(removeSample))
            return "FAILURE: removeSample is not using a valid format";
        if(!Whitelists.textMatches(igoUser))
            return "FAILURE: igoUser is not using a valid format";

       boolean isForce = false;
       if(force.toUpperCase().equals("TRUE")){
            isForce = true;
       }
       AddPoolToLane task = new AddPoolToLane(flowcell, sample, removeSample, igoUser, Long.parseLong(lane), isForce, conn);
       return task.execute();
    }
}