package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.AddOrCreateSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;


@RestController
@RequestMapping("/")
public class AddSampleSet {
    private static Log log = LogFactory.getLog(AddSampleSet.class);
    private final ConnectionPoolLIMS conn;
    private final AddOrCreateSet task = new AddOrCreateSet();

    public AddSampleSet(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/addSampleSet")
    public String getContent(@RequestParam(value = "igoId", required = false) String[] igoId,
                             @RequestParam(value = "user") String user,
                             @RequestParam(value = "pair", required = false) String[] pair,
                             @RequestParam(value = "category", required = false) String[] category,
                             @RequestParam(value = "project", required = false) String[] request,
                             @RequestParam(value = "igoUser") String igoUser,
                             @RequestParam(value = "setName", required = false) String name,
                             @RequestParam(value = "mapName", required = false) String mapName,
                             @RequestParam(value = "baitSet", required = false) String baitSet,
                             @RequestParam(value = "primeRecipe", required = false) String primeRecipe,
                             @RequestParam(value = "primeRequest", required = false) String primeRequest,
                             @RequestParam(value = "externalSpecimen", required = false) String[] externalSpecimen) {
        try {
            log.info("Adding to sample set " + name + " at a request by user " + user);
            if (igoId != null) {
                for (int i = 0; i < igoId.length; i++) {
                    if (!Whitelists.sampleMatches(igoId[i]))
                        return "FAILURE: igoId is not using a valid format";
                }
            }
            if (!Whitelists.textMatches(igoUser))
                return "FAILURE: igoUser is not using a valid format";

            if (!Whitelists.textMatches(name)) {
                return "FAILURE: setName is not using a valid format";
            }
            if (request != null) {
                for (int i = 0; i < request.length; i++) {
                    if (!Whitelists.requestMatches(request[i]))
                        return "FAILURE: project is not using a valid format";
                }
            }
            if (!Whitelists.textMatches(baitSet)) {
                return "FAILURE: baitSet is not using a valid format";
            }
            if (!Whitelists.textMatches(primeRecipe)) {
                return "FAILURE: primeRecipe is not using a valid format";
            }

            if (externalSpecimen != null) {
                for (String spec : externalSpecimen) {
                    if (!Whitelists.textMatches(spec)) {
                        return "FAILURE: externalSpecimen is not using a valid format";
                    }
                }
            }
            task.init(igoUser, name, mapName, request, igoId, pair, category, baitSet, primeRecipe, primeRequest,
                    externalSpecimen);
            Future<Object> result = conn.submitTask(task);
            try {
                return "Record Id:" + result.get();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return "ERROR IN ADDING TO SAMPLE SET: " + e.getMessage();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }
}