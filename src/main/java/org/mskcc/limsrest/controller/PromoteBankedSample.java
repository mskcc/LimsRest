package org.mskcc.limsrest.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.LimsException;
import org.mskcc.limsrest.service.PromoteBanked;
import org.mskcc.limsrest.util.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.PrintWriter;
import java.io.StringWriter;

@RestController
@RequestMapping("/")
public class PromoteBankedSample {
    private final static Log log = LogFactory.getLog(PromoteBankedSample.class);
    private final ConnectionLIMS conn;

    public PromoteBankedSample(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping("/promoteBankedSample")  // POST called by REX
    public ResponseEntity<String> getContent(@RequestParam(value = "bankedId") String[] bankedId,
                                             @RequestParam(value = "user") String user,
                                             @RequestParam(value = "requestId", defaultValue = "NULL") String request,
                                             @RequestParam(value = "projectId", defaultValue = "NULL") String project,
                                             @RequestParam(value = "serviceId") String service,
                                             @RequestParam(value = "igoUser") String igoUser,
                                             @RequestParam(value = "materials", defaultValue = "NULL") String materials,
                                             @RequestParam(value = "dryrun", defaultValue = "false") boolean dryrun) {
        log.info("Starting /promoteBankedSample " + bankedId[0] + " to requestId:" + request + ":"
                + project + ",serviceId:" + service + ", igoUser: " + igoUser + " dryrun:" + dryrun);

        for (String bid : bankedId) {
            if (!Whitelists.sampleMatches(bid))
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bankedId " + bid + " is not using a valid " +
                        "format. " + Whitelists.sampleFormatText());
        }

        if (!Whitelists.textMatches(igoUser))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("igoUser is not using a valid format. " + Whitelists.textFormatText());

        if (!Whitelists.textMatches(materials))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("material is not using a valid format. " + Whitelists.textFormatText());

        if (!Whitelists.requestMatches(request))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("requestId is not using a valid format. " + Whitelists.requestFormatText());

        if (!Whitelists.requestMatches(project))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("projectId is not using a valid format. " + Whitelists.requestFormatText());

        if (!Whitelists.serviceMatches(service))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("serviceId is not using a valid format. " + Whitelists.serviceFormatText());

        PromoteBanked task = new PromoteBanked(bankedId, project, request, service, igoUser, materials, dryrun, conn);
        log.info("Starting promote");
        try {
            ResponseEntity<String> returnCode = task.execute();

            if (returnCode.getHeaders().containsKey(Constants.ERRORS)) {
                String errors = StringUtils.join(returnCode.getHeaders().get(Constants.ERRORS), ",");
                log.error("Errors-" + errors);
                throw new LimsException(errors);
            }
            return returnCode;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.error("Promote error:" + e + "\nTRACE: " + sw.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}