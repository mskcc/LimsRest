package org.mskcc.limsrest.web;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.LimsException;
import org.mskcc.limsrest.limsapi.PromoteBanked;
import org.mskcc.limsrest.staticstrings.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class PromoteBankedSample {
    private final static Log log = LogFactory.getLog(PromoteBankedSample.class);
    private final ConnectionQueue connQueue;
    private final PromoteBanked task;


    public PromoteBankedSample(ConnectionQueue connQueue, PromoteBanked banked) {
        this.connQueue = connQueue;
        this.task = banked;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/promoteBankedSample")
    public ResponseEntity<String> getContent(@RequestParam(value = "bankedId") String[] bankedId,
                                             @RequestParam(value = "user") String user,
                                             @RequestParam(value = "requestId", defaultValue = "NULL") String request,
                                             @RequestParam(value = "projectId", defaultValue = "NULL") String project,
                                             @RequestParam(value = "serviceId") String service,
                                             @RequestParam(value = "igoUser") String igoUser,
                                             @RequestParam(value = "dryrun", defaultValue = "false") String dryrun) {
        log.info("Starting to promote banked sample " + bankedId[0] + " to request " + request + ":" + service + ":"
                + project + " by service " + user + " and igo user: " + igoUser);

        for (String bid : bankedId) {
            if (!Whitelists.sampleMatches(bid))
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bankedId " + bid + " is not using a valid " +
                        "format. " + Whitelists.sampleFormatText());
        }

        if (!Whitelists.textMatches(igoUser))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("igoUser is not using a valid format. " + Whitelists
                    .textFormatText());

        if (!Whitelists.requestMatches(request))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("requestId is not using a valid format. " + Whitelists
                    .requestFormatText());


        if (!Whitelists.requestMatches(project))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("projectId is not using a valid format. " + Whitelists.requestFormatText());

        if (!Whitelists.serviceMatches(service))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("serviceId is not using a valid format. " + Whitelists.serviceFormatText());

        dryrun = dryrun.toLowerCase();
        log.info("Creating task");
        task.init(bankedId, project, request, service, igoUser, dryrun);
        log.info("Getting result");
        Future<Object> result = connQueue.submitTask(task);

        ResponseEntity<String> returnCode;

        try {
            log.info("Waiting for result");
            returnCode = (ResponseEntity<String>) result.get();

            if (returnCode.getHeaders().containsKey(Constants.ERRORS) && !dryrun.equals("true")) {
                throw new LimsException(StringUtils.join(returnCode.getHeaders().get(Constants.ERRORS), ","));
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage() + "\nTRACE: " + sw.toString());
        }

        return returnCode;
    }
}