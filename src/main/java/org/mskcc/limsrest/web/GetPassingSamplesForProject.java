package org.mskcc.limsrest.web;

import java.util.concurrent.Future;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import org.mskcc.limsrest.limsapi.*;
import org.mskcc.limsrest.connection.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


@RestController
public class GetPassingSamplesForProject{

    private final ConnectionQueue connQueue; 
    private final GetPassingSamples task;
    private Log log = LogFactory.getLog(GetPassingSamplesForProject.class);

    public GetPassingSamplesForProject( ConnectionQueue connQueue, GetPassingSamples getPassing ){
        this.connQueue = connQueue;
        this.task = getPassing;
    }


    @RequestMapping("/getPassingSamplesForProject")
        public RequestSummary getContent(@RequestParam(value="project") String project, @RequestParam(value="user") String user,
                @RequestParam(value="year", required=false) String year, @RequestParam(value="month", required=false) String month, @RequestParam(value="day", required=false) String day) {
            RequestSummary rs = new RequestSummary();
            Integer dayParam = null;
            Integer monthParam = null;
            Integer yearParam = null;
            if(!((year == null && month == null && day == null) || (year != null && month != null && day != null))){
                rs.setRestStatus("ERROR: If any of the date fields day, month, year are included, they must all be included");
                return rs;
            } else if(year != null){
                dayParam = Integer.valueOf(day);
                monthParam = Integer.valueOf(month);
                yearParam = Integer.valueOf(year);
            }
            Whitelists wl = new Whitelists();
            if(!wl.requestMatches(project)){
                 rs.setRestStatus("ERROR: projectis not using a valid format"); 
                 return rs;
            }
            log.info("Starting to get passing samples for project " + project + " for user " + user);
            task.init(project, dayParam, monthParam, yearParam);
            Future<Object> result = connQueue.submitTask(task);
            try{
                rs = (RequestSummary)result.get();
            } catch(Exception e){
                rs.setRestStatus(e.getMessage());
            }
            return rs;
        }



}

