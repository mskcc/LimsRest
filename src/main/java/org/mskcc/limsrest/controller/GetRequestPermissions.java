package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetRequestPermissionsTask;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

/**
 * Used to query what permissions should be granted to fastqs at the time of delivery:
 * - returns the "PI" aka "LAB NAME" such as 'peerd'
 * - returns the groups that need read access whether it is a CMO project, BIC project and data access emails
 */
@RestController
@RequestMapping("/")
public class GetRequestPermissions {
    private final static Log log = LogFactory.getLog(GetRequestPermissions.class);

    private final ConnectionLIMS conn;

    public GetRequestPermissions(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getRequestPermissions")
    public GetRequestPermissionsTask.RequestPermissions getContent(@RequestParam(value = "request") String requestId, HttpServletRequest request) {
        log.info("/getRequestPermissions for request:" + requestId + " " + request.getRemoteAddr());

        if (!Whitelists.requestMatches(requestId)) {
            log.error("FAILURE: requestId is not using a valid format.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FAILURE: requestId is not using a valid format.");
        }

        try {
            GetRequestPermissionsTask t = new GetRequestPermissionsTask(requestId, conn);
            GetRequestPermissionsTask.RequestPermissions x = t.execute();
            if (x == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, requestId + " Request Not Found");
            }
            return x;
        } catch (Exception e) {
            log.error(e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}