package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.List;
import java.util.LinkedList;

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
public class GetBarcodeSequence{

    private final ConnectionQueue connQueue; 
    private final FindBarcodeSequence task;
   private Log log = LogFactory.getLog(GetBarcodeSequence.class);

    public GetBarcodeSequence( ConnectionQueue connQueue, FindBarcodeSequence findSeq){
        this.connQueue = connQueue;
        this.task = findSeq;
    }



    @RequestMapping("/getBarcodeSequence")
    public String getContent(@RequestParam(value="user") String user, 
                          @RequestParam(value="barcodeId") String barcodeId) {                          
        Whitelists wl = new Whitelists();
        if(!wl.textMatches(barcodeId))
            return "FAILURE: flowcell is not using a valid format";

       task.init(barcodeId);
       log.info("Starting get barcode sequence " + barcodeId);
       Future<Object> result = connQueue.submitTask(task);
       String seq = ""; 
       try{
         seq = (String)result.get();
       } catch(Exception e){
       }
       return seq;
    }

}

