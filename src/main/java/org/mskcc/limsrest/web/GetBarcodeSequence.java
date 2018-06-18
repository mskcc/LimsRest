package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.FindBarcodeSequence;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

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
        if(!Whitelists.textMatches(barcodeId))
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