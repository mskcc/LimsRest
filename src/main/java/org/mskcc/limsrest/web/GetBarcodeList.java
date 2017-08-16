package org.mskcc.limsrest.web;

import java.util.concurrent.Future;
import java.util.List;
import java.util.LinkedList;
import java.io.PrintWriter;
import java.io.StringWriter;

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
public class GetBarcodeList {

    private final ConnectionQueue connQueue; 
    private final GetBarcodeInfo task;
//    private static final Logger logger = LoggerFactory.getLogger(GetProjectQc.class);
   private Log log = LogFactory.getLog(LimsTask.class);

    public GetBarcodeList( ConnectionQueue connQueue, GetBarcodeInfo barcodes){
        this.connQueue = connQueue;
        this.task = barcodes;
    }



    @RequestMapping("/getBarcodeList")
    public List<String> getContent(@RequestParam(value="user") String user) {
       log.info("Starting get barcode list for user" + user);
       Future<Object> result = connQueue.submitTask(task);
       List<String> values = new LinkedList<>(); 
       try{
         values = (List<String>)result.get();
       } catch(Exception e){
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         log.info(e.getMessage() + " TRACE: " + sw.toString()); 
       }
       return values;
    }

}

