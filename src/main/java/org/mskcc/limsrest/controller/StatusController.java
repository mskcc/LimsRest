package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.StatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/")
public class StatusController {
    private static Log log = LogFactory.getLog(StatusController.class);

    private final ConnectionLIMS conn;

    @Value("${spring.application.name:LimsRest}")
    private String applicationName;

    public StatusController(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/status")
    public ResponseEntity<StatusResponse> getStatus() {
        log.info("Status check endpoint called");
        
        StatusResponse status = new StatusResponse();
        status.setApplication(applicationName);
        status.setStatus("UP");
        status.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    public ResponseEntity<StatusResponse> getHealth() {
        log.info("Health check endpoint called");
        
        StatusResponse health = new StatusResponse();
        health.setApplication(applicationName);
        health.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Check LIMS connection
        boolean limsConnected = false;
        String limsStatus = "DOWN";
        try {
            if (conn != null && conn.getConnection() != null && conn.getConnection().isConnected()) {
                limsConnected = true;
                limsStatus = "UP";
            }
        } catch (Exception e) {
            log.error("Error checking LIMS connection: " + e.getMessage());
            limsStatus = "ERROR: " + e.getMessage();
        }
        
        health.setLimsConnection(limsStatus);
        health.setOverallStatus(limsConnected ? "UP" : "DEGRADED");
        
        // Return appropriate HTTP status
        HttpStatus httpStatus = limsConnected ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(health);
    }

    @GetMapping("/ping")
    public ResponseEntity<StatusResponse> ping() {
        StatusResponse response = new StatusResponse();
        response.setMessage("pong");
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return ResponseEntity.ok(response);
    }
} 