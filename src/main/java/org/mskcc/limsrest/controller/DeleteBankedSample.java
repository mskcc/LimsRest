package org.mskcc.limsrest.controller;

import com.velox.api.util.ServerException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.DeleteBankedTask;
import org.mskcc.limsrest.util.Messages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.rmi.RemoteException;


@RestController
@RequestMapping("/")
public class DeleteBankedSample {
    private static Log log = LogFactory.getLog(DeleteBankedSample.class);

    private final ConnectionLIMS conn;
   
    public DeleteBankedSample( ConnectionLIMS conn){
        this.conn = conn;
    }

    @GetMapping("/deleteBankedSample")
    public ResponseEntity<String>  getContent(@RequestParam(value="userId") String userId, @RequestParam(value="serviceId") String serviceId, @RequestParam(value="user") String user) throws ServerException, RemoteException {
       log.info("Starting /deleteBankedSample " + userId + " from service request " + serviceId +  " by " + user  );
       if (!Whitelists.sampleMatches(userId))
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("userId is not using a valid format. " + Whitelists.sampleFormatText() );

       if (!Whitelists.serviceMatches(serviceId))
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("serviceId is not using a valid igo ilabs request");

       DeleteBankedTask task = new DeleteBankedTask(userId, serviceId, conn);

       String returnCode = task.execute();
       if (!returnCode.equals(Messages.SUCCESS)){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(returnCode);
       }
       return ResponseEntity.ok(returnCode);
    }

    @GetMapping("/deleteBankedService")
    public ResponseEntity<String>  getContent(@RequestParam(value="serviceId") String serviceId, @RequestParam(value="user") String user) throws ServerException, RemoteException {
       log.info("Starting to delete banked samples and request for the service " + serviceId +  " by " + user  );

       if (!Whitelists.serviceMatches(serviceId))
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("serviceId is not using a valid igo ilabs request");

        DeleteBankedTask task = new DeleteBankedTask(null, serviceId, conn);

        String returnCode = task.execute();
        if (!returnCode.equals(Messages.SUCCESS)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(returnCode);
        }
        return ResponseEntity.ok(returnCode);
    }
}