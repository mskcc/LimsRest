package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.dmp.DefaultTodayDateParser;
import org.mskcc.limsrest.service.dmp.GenerateBankedSamplesFromDMP;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class CreateBankedSamplesFromDMP {
    private static final Log log = LogFactory.getLog(CreateBankedSamplesFromDMP.class);

    private final ConnectionPoolLIMS conn;
    private final GenerateBankedSamplesFromDMP generateBankedSamplesFromDMP = new GenerateBankedSamplesFromDMP();

    public CreateBankedSamplesFromDMP(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping("/createBankedSamplesFromDMP")
    public ResponseEntity<String> createBankedSamplesFromDMP(@RequestParam(value = "date", required = false) String date) {
        LocalDate localDate = null;
        try {
            localDate = DefaultTodayDateParser.parse(date);
            log.info(String.format("Starting to create banked samples from DMP samples for date: %s", localDate));
            log.info("Creating task");

            generateBankedSamplesFromDMP.setDate(localDate);

            log.info("Getting result");
            Future<Object> result = conn.submitTask(generateBankedSamplesFromDMP);

            String response = (String) result.get();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            String message = String.format("Unable to create Banked Samples from DMP Samples for date: %s",
                    getDateUsed(date, localDate));
            log.error(message, e);

            ResponseEntity<String> responseEntity = new ResponseEntity<>(message + " Cause: " + e.getMessage(),
                    HttpStatus.NOT_FOUND);
            return responseEntity;
        }
    }

    private Object getDateUsed(@RequestParam(value = "date", required = false) String date, LocalDate localDate) {
        if (localDate == null) {
            if (date == null)
                return "no date provided and unable to resolve default date";
            return date;
        }

        return localDate;
    }
}