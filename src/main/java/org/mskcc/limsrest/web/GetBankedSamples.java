package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionPoolLIMS;
import org.mskcc.limsrest.limsapi.GetBanked;
import org.mskcc.limsrest.limsapi.LimsException;
import org.mskcc.limsrest.limsapi.SampleSummary;
import org.mskcc.limsrest.staticstrings.Messages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

/**
 One endpoint called by Rex.mskcc.org, all endpoints invoked by REX are:
 <BR>
  getBankedSamples
  promoteBankedSample
  setBankedSample
  getPickListValues
  getIntakeTerms
  getBarcodeList
  pairingInfo
  getAllStudies
  getPmProject
  addSampleSet
 */
@RestController
@RequestMapping("/")
public class GetBankedSamples {
    private static Log log = LogFactory.getLog(GetBankedSamples.class);
    private final ConnectionPoolLIMS conn;
    private final GetBanked task;


    public GetBankedSamples(ConnectionPoolLIMS conn, GetBanked project) {
        this.conn = conn;
        this.task = project;
    }

    @GetMapping("/getBankedSamples")
    public ResponseEntity<List<SampleSummary>> getContent(@RequestParam(value = "project", required = false) String
                                                                  project,
                                                          @RequestParam(value = "userId", required = false) String[]
                                                                  userId,
                                                          @RequestParam(value = "serviceId", required = false) String
                                                                  serviceRequest,
                                                          @RequestParam(value = "investigator", required = false)
                                                                  String investigator) {
        LinkedList<SampleSummary> samples = new LinkedList<>();
        if (project != null) {
            if (!Whitelists.requestMatches(project)) {
                log.info("FAILURE: project is not using a valid format");
                return new ResponseEntity(samples, HttpStatus.BAD_REQUEST);
            }
            log.info("Getting banked samples for project: " + project);
            task.init(project);
        }
        if (userId != null) {
            log.info("Getting banked samples for name: " + userId[0]);
            for (int i = 0; i < userId.length; i++) {
                if (!Whitelists.sampleMatches(userId[i])) {
                    log.info("FAILURE: userId is not using a valid format");
                    return new ResponseEntity(samples, HttpStatus.BAD_REQUEST);
                }
            }
            task.init(userId);
        }
        if (serviceRequest != null) {
            if (!Whitelists.textMatches(serviceRequest)) {
                log.info("FAILURE: serviceRequest is not using a valid format");
                return new ResponseEntity(samples, HttpStatus.BAD_REQUEST);
            }
            log.info("Getting banked samples for service: " + serviceRequest);
            task.initServiceId(serviceRequest);
        }
        if (investigator != null) {
            if (!Whitelists.textMatches(investigator)) {
                log.info("FAILURE: investigator is not using a valid format: " + investigator);
                return new ResponseEntity(samples, HttpStatus.BAD_REQUEST);
            }
            log.info("Getting banked samples for investigator: " + investigator);
            task.initInvestigator(investigator);
        }

        Future<Object> result = conn.submitTask(task);
        try {
            samples = (LinkedList<SampleSummary>) result.get();
            if (samples.size() > 0) {
                SampleSummary possibleSentinelValue = samples.peekFirst();
                if (possibleSentinelValue.getCmoId() != null && possibleSentinelValue.getCmoId().startsWith(Messages
                        .ERROR_IN)) {
                    throw new LimsException(possibleSentinelValue.getCmoId());
                }
            }
        } catch (Exception e) {
            return new ResponseEntity(samples, HttpStatus.BAD_REQUEST);

        }
        return ResponseEntity.ok(samples);
    }
}