package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetBanked;
import org.mskcc.limsrest.service.LimsException;
import org.mskcc.limsrest.service.SampleSummary;
import org.mskcc.limsrest.util.Messages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;

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
    private final ConnectionLIMS conn;

    public GetBankedSamples(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @RequestMapping("/getBankedSamples")
    public ResponseEntity<List<SampleSummary>> getContent(@RequestParam(value = "project", required = false) String
                                                                  project,
                                                          @RequestParam(value = "userId", required = false) String[]
                                                                  userId,
                                                          @RequestParam(value = "serviceId", required = false) String
                                                                  serviceRequest,
                                                          @RequestParam(value = "investigator", required = false)
                                                                  String investigator) {
        GetBanked task = new GetBanked(conn);
        LinkedList<SampleSummary> samples = new LinkedList<>();
        if (project != null) {
            if (!Whitelists.requestMatches(project)) {
                log.info("FAILURE: project is not using a valid format");
                return new ResponseEntity(samples, HttpStatus.BAD_REQUEST);
            }
            log.info("Getting banked samples for project: " + project);
            task.setProject(project);
        }
        if (userId != null) {
            log.info("Getting banked samples for name: " + userId[0]);
            for (int i = 0; i < userId.length; i++) {
                if (!Whitelists.sampleMatches(userId[i])) {
                    log.info("FAILURE: userId is not using a valid format");
                    return new ResponseEntity(samples, HttpStatus.BAD_REQUEST);
                }
            }
            task.setNames(userId);
        }
        if (serviceRequest != null) {
            if (!Whitelists.textMatches(serviceRequest)) {
                log.info("FAILURE: serviceRequest is not using a valid format");
                return new ResponseEntity(samples, HttpStatus.BAD_REQUEST);
            }
            log.info("Getting banked samples for service: " + serviceRequest);
            task.setServiceId(serviceRequest);
        }
        if (investigator != null) {
            if (!Whitelists.textMatches(investigator)) {
                log.info("FAILURE: investigator is not using a valid format: " + investigator);
                return new ResponseEntity(samples, HttpStatus.BAD_REQUEST);
            }
            log.info("Getting banked samples for investigator: " + investigator);
            task.setInvestigator(investigator);
        }

        try {
            samples = task.execute();
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