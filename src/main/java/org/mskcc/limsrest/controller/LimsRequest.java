package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.GetRequest;
import org.mskcc.limsrest.service.RequestDetailed;
import org.mskcc.limsrest.service.SetRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;


@RestController
@RequestMapping("/") @Deprecated
public class LimsRequest {
    private static Log log = LogFactory.getLog(LimsRequest.class);
    private final ConnectionPoolLIMS conn;
    private final GetRequest reader;
   
    public LimsRequest(ConnectionPoolLIMS conn, SetRequest setter, GetRequest reader){
        this.conn = conn;
        this.reader = reader;
    }

    @RequestMapping(value = "/limsRequest", method = RequestMethod.GET)
    public ResponseEntity<List<RequestDetailed>> setContent(@RequestParam(value = "request") String[] request, @RequestParam(value = "igoUser") String igoUser,
                                                            @RequestParam(value = "user") String user,
                                                            @RequestParam(value = "field", defaultValue = "NULL") String[] fieldName) {
        LinkedList<RequestDetailed> reqSummary = new LinkedList<>();
        log.info("/limsRequest Getting a request " + request[0] + " for field" + fieldName[0]);
        for (String r : request) {
            if (!Whitelists.requestMatches(r)) {
                return new ResponseEntity(reqSummary, HttpStatus.BAD_REQUEST);
            }
        }
        for (String f : fieldName) {
            if (!Whitelists.textMatches(igoUser))
                return new ResponseEntity(reqSummary, HttpStatus.BAD_REQUEST);
        }
        reader.init(igoUser, request, fieldName);
        Future<Object> result = conn.submitTask(reader);
        try {
            reqSummary = (LinkedList<RequestDetailed>) result.get();
        } catch (Exception e) {
            return new ResponseEntity(reqSummary, HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(reqSummary);
    }
}