package org.mskcc.limsrest.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.LimsException;
import org.mskcc.limsrest.limsapi.SetOrCreateBanked;
import org.mskcc.limsrest.staticstrings.Messages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Future;

@RestController
public class SetBankedSample {
    private final ConnectionQueue connQueue;
    private final SetOrCreateBanked task;
    private Log log = LogFactory.getLog(SetBankedSample.class);

    public SetBankedSample(ConnectionQueue connQueue, SetOrCreateBanked banked) {
        this.connQueue = connQueue;
        this.task = banked;
    }

    //userId refers to the the Sample.UserId in the lims
    @RequestMapping("/setBankedSample")
    public ResponseEntity<String> getContent(
            @RequestParam(value = "userId") String userId,
            @RequestParam(value = "user") String user,
            @RequestParam(value = "igoUser") String igoUser,
            @RequestParam(value = "index", defaultValue = "NULL") String barcodeId,
            @RequestParam(value = "barcodePosition", defaultValue = "NULL") String barcodePosition,
            @RequestParam(value = "vol", defaultValue = "-1.0") String vol,
            @RequestParam(value = "estimatedPurity", required = false) String estimatedPurity,
            @RequestParam(value = "concentration", defaultValue = "-1.0") String concentration,
            @RequestParam(value = "concentrationUnits", defaultValue = "NULL") String concentrationUnits,
            @RequestParam(value = "sequencingReadLength", defaultValue = "NULL") String sequencingReadLength,
            @RequestParam(value = "numTubes", defaultValue = "NULL") String numTubes,
            @RequestParam(value = "assay", required = false) String[] assay,
            @RequestParam(value = "clinicalInfo", defaultValue = "NULL") String clinicalInfo,
            @RequestParam(value = "collectionYear", defaultValue = "NULL") String collectionYear,
            @RequestParam(value = "gender", defaultValue = "NULL") String gender,
            @RequestParam(value = "knownGeneticAlteration", defaultValue = "NULL") String geneticAlterations,
            @RequestParam(value = "rowIndex") String rowIndex,
            @RequestParam(value = "transactionId") String transactionId,
            @RequestParam(value = "organism", defaultValue = "NULL") String organism,
            @RequestParam(value = "platform", defaultValue = "NULL") String platform,
            @RequestParam(value = "preservation", defaultValue = "NULL") String preservation,
            @RequestParam(value = "specimenType", defaultValue = "NULL") String specimenType,
            @RequestParam(value = "sampleType", defaultValue = "NULL") String sampleType,
            @RequestParam(value = "sampleOrigin", defaultValue = "NULL") String sampleOrigin,
            @RequestParam(value = "micronicTubeBarcode", defaultValue = "NULL") String micronicTubeBarcode,
            @RequestParam(value = "sampleClass", defaultValue = "NULL") String sampleClass,
            @RequestParam(value = "requestedReads", required = false) String requestedReads,
            @RequestParam(value = "spikeInGenes", defaultValue = "NULL") String spikeInGenes,
            @RequestParam(value = "tissueType", defaultValue = "NULL") String tissueType,
            @RequestParam(value = "cancerType", defaultValue = "") String cancerType,
            @RequestParam(value = "recipe", defaultValue = "NULL") String recipe,
            @RequestParam(value = "runType", defaultValue = "NULL") String runType,
            @RequestParam(value = "investigator", defaultValue = "NULL") String investigator,
            @RequestParam(value = "cellCount", defaultValue = "NULL") String cellCount,
            @RequestParam(value = "naToExtract", defaultValue = "NULL") String naToExtract,
            @RequestParam(value = "serviceId") String serviceId,
            @RequestParam(value = "coverage", defaultValue = "NULL") String coverage,
            @RequestParam(value = "seqRequest", defaultValue = "NULL") String seqRequest,
            @RequestParam(value = "rowPos", defaultValue = "NULL") String rowPos,
            @RequestParam(value = "colPos", defaultValue = "NULL") String colPos,
            @RequestParam(value = "plateId", defaultValue = "NULL") String plateId,
            @RequestParam(value = "tubeId", defaultValue = "NULL") String tubeId,
            @RequestParam(value = "patientId", defaultValue = "NULL") String patientId,
            @RequestParam(value = "normalizedPatientId", defaultValue = "NULL") String normalizedPatientId,
            @RequestParam(value = "cmoPatientId", defaultValue = "NULL") String cmoPatientId) {

        log.info("Starting to set banked sample " + userId + " by user " + user);
        if (assay == null) {
            assay = new String[1];
            assay[0] = "NULL";
        }

        Double ep = null;
        if (estimatedPurity != null) {
            try {
                ep = new Double(estimatedPurity);
            } catch (NumberFormatException nfe) {
                ep = 0.0d;
            }
        }

        if (!Whitelists.textMatches(serviceId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request is not using a valid format");
        }
        if (!Whitelists.sampleMatches(userId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("userId is not using a valid format");
        }

        task.init(
                igoUser,
                investigator,
                userId,
                assay,
                clinicalInfo,
                collectionYear,
                concentrationUnits,
                gender,
                geneticAlterations,
                organism,
                platform,
                preservation,
                specimenType,
                sampleType,
                sampleOrigin,
                sampleClass,
                spikeInGenes,
                tissueType,
                cancerType,
                micronicTubeBarcode,
                barcodeId,
                barcodePosition,
                recipe,
                runType,
                serviceId,
                tubeId,
                patientId,
                normalizedPatientId,
                cmoPatientId,
                rowPos,
                colPos,
                plateId,
                requestedReads,
                cellCount,
                sequencingReadLength,
                numTubes,
                naToExtract,
                ep,
                Float.parseFloat(vol),
                Double.parseDouble(concentration),
                Integer.parseInt(rowIndex),
                Long.parseLong(transactionId));

        Future<Object> result = connQueue.submitTask(task);
        String returnCode;

        try {
            returnCode = (String) result.get();
            if (returnCode.startsWith(Messages.ERROR_IN)) {
                throw new LimsException(returnCode);
            }
            returnCode = "Record Id:" + returnCode;
        } catch (Exception e) {
            log.error(e);
            returnCode = e.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(returnCode);
        }

        return ResponseEntity.ok(returnCode);
    }
}

