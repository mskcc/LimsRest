package org.mskcc.limsrest.web;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.*;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.GenerateSampleCmoIdTask;
import org.mskcc.limsrest.staticstrings.Constants;
import org.mskcc.limsrest.util.Utils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

//@RestController TODO
public class GetCorrectedSampleCmoId {
    private static final Log log = LogFactory.getLog(GetCorrectedSampleCmoId.class);
    private final static String DMP_SUFFIX = "Z";
    private final ConnectionQueue connQueue;
    private final GenerateSampleCmoIdTask task;

    public GetCorrectedSampleCmoId(ConnectionQueue connQueue, GenerateSampleCmoIdTask generateSampleCmoIdTask) {
        this.connQueue = connQueue;
        this.task = generateSampleCmoIdTask;
    }

    /**
     * This function retrieves information about sample from lims and generated CMO Sample Id.
     *
     * @param sampleIgoId
     * @return CMO Sample Id
     */
    @GetMapping("/getSampleCmoId")
    public ResponseEntity<String> getSampleCmoIdByIgoId(@RequestParam(value = "sampleIgoId") String sampleIgoId) {
        try {
            validateSampleId(sampleIgoId);

            log.info(String.format("Starting to generate sample cmo id for sample igo id: %s", sampleIgoId));

            log.info("Creating Generate sample cmo id task");
            task.init(sampleIgoId);

            log.info("Getting result of Generate sample cmo id task");
            Future<Object> result = connQueue.submitTask(task);

            String correctedSampleCmoId = (String) result.get();

            log.info(String.format("Generated CMO Sample ID: %s", correctedSampleCmoId));
            return ResponseEntity.ok(correctedSampleCmoId);
        } catch (Exception e) {
            log.error(String.format("Error while generating CMO Sample Id for sample: %s", sampleIgoId), e);

            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add(Constants.ERRORS, String.format("Error while generating CMO Sample Id for sample: %s. Cause: " +
                    "%s", sampleIgoId, ExceptionUtils.getRootCauseMessage(e)));

            return new ResponseEntity<>(headers, HttpStatus.OK);
        }
    }

    /**
     * This function generates CMO Sample Id for DMP Samples from parameters passed in a query
     *
     * @param sampleId
     * @param requestId
     * @param patientId
     * @param sampleClass
     * @param sampleOrigin
     * @param specimenType
     * @param nucleidAcid
     * @param counter
     * @return CMO Sample Id for DMP Samples
     */
    @RequestMapping("/getDmpSampleCmoId")
    public ResponseEntity<String> getDMPSampleCmoIdByCmoSampleView(
            @RequestParam String sampleId,
            @RequestParam(required = false) String requestId,
            @RequestParam String patientId,
            @RequestParam String sampleClass,
            @RequestParam String sampleOrigin,
            @RequestParam String specimenType,
            @RequestParam String nucleidAcid,
            @RequestParam int counter) {

        CorrectedCmoSampleView correctedCmoSampleView = new CorrectedCmoSampleView(sampleId);

        try {
            validateSampleId(sampleId);
            correctedCmoSampleView.setSampleId(sampleId);
            correctedCmoSampleView.setRequestId(requestId);
            correctedCmoSampleView.setPatientId(patientId);
            correctedCmoSampleView.setSampleClass(SampleClass.fromValue(sampleClass));
            correctedCmoSampleView.setSampleOrigin(SampleOrigin.fromValue(sampleOrigin));
            correctedCmoSampleView.setSpecimenType(SpecimenType.fromValue(specimenType));
            correctedCmoSampleView.setNucleidAcid(NucleicAcid.fromValue(nucleidAcid));
            correctedCmoSampleView.setCounter(counter);

            log.info(String.format("Starting to generate sample cmo id for cmo sample: %s", correctedCmoSampleView));

            log.info("Creating Generate sample cmo id task");
            task.init(correctedCmoSampleView);

            log.info("Getting result of Generate sample cmo id task");
            Future<Object> result = connQueue.submitTask(task);

            String correctedSampleCmoId = (String) result.get();
            log.info(String.format("Generated CMO Sample ID: %s", correctedSampleCmoId));

            String dmpCmoId = String.format("%s%s", correctedSampleCmoId, DMP_SUFFIX);
            log.info(String.format("Formatted DMP CMO Sample ID: %s", dmpCmoId));

            return ResponseEntity.ok(dmpCmoId);
        } catch (Exception e) {
            log.error(String.format("Error while generating CMO Sample Id for cmo sample view: %s",
                    correctedCmoSampleView), e);

            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add(Constants.ERRORS, String.format("Error while generating CMO Sample Id for sample: %s. Cause: " +
                    "%s", correctedCmoSampleView, ExceptionUtils.getRootCauseMessage(e)));

            return new ResponseEntity<>(headers, HttpStatus.OK);
        }
    }

    private void validateSampleId(String sampleIgoId) {
        if (!Whitelists.sampleMatches(sampleIgoId))
            throw new IncorrectSampleIgoIdFormatException(String.format("Sample igo id provided %s is in incorrect " +
                    "format. Expected " +
                    "format: %s", sampleIgoId, Whitelists.SAMPLE_NAME_PATTERN));
    }

    /**
     * This function generates CMO Sample Id from parameters passed to a query
     *
     * @param igoId
     * @param userSampleId
     * @param requestId
     * @param patientId
     * @param sampleClass
     * @param sampleOrigin
     * @param specimenType
     * @param sampleType
     * @param currentCmoSampleId
     * @param nucleidAcid
     * @return CMO Sample Id
     */
    @RequestMapping("/getSampleCmoIdFromParams")
    public ResponseEntity<String> getSampleCmoIdByCmoSampleView(
            @RequestParam String igoId,
            @RequestParam String userSampleId,
            @RequestParam(required = false) String requestId,
            @RequestParam String patientId,
            @RequestParam String sampleClass,
            @RequestParam String sampleOrigin,
            @RequestParam String specimenType,
            @RequestParam String sampleType,
            @RequestParam String currentCmoSampleId,
            @RequestParam String nucleidAcid) {

        CorrectedCmoSampleView correctedCmoSampleView = new CorrectedCmoSampleView(igoId);

        try {
            validateSampleId(igoId);
            correctedCmoSampleView.setSampleId(userSampleId);
            correctedCmoSampleView.setRequestId(requestId);
            correctedCmoSampleView.setPatientId(patientId);
            correctedCmoSampleView.setSampleClass(SampleClass.fromValue(sampleClass));
            correctedCmoSampleView.setSampleOrigin(SampleOrigin.fromValue(sampleOrigin));
            correctedCmoSampleView.setSpecimenType(SpecimenType.fromValue(specimenType));
            correctedCmoSampleView.setSampleType(SampleType.fromString(sampleType));
            correctedCmoSampleView.setCorrectedCmoId(currentCmoSampleId);
            Utils.getOptionalNucleicAcid(nucleidAcid, igoId).ifPresent(correctedCmoSampleView::setNucleidAcid);

            log.info(String.format("Starting to generate sample cmo id for sample: %s", correctedCmoSampleView));

            log.info("Creating Generate sample cmo id task");
            task.init(correctedCmoSampleView);

            log.info("Getting result of Generate sample cmo id task");
            Future<Object> result = connQueue.submitTask(task);

            String correctedSampleCmoId = (String) result.get();
            log.info(String.format("Generated CMO Sample ID: %s", correctedSampleCmoId));

            return ResponseEntity.ok(correctedSampleCmoId);
        } catch (Exception e) {
            log.error(String.format("Error while generating CMO Sample Id for cmo sample view: %s",
                    correctedCmoSampleView), e);

            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("ERRORS", String.format("Error while generating CMO Sample Id for sample: %s. Cause: " +
                    "%s", correctedCmoSampleView, ExceptionUtils.getRootCauseMessage(e)));

            return new ResponseEntity<>(headers, HttpStatus.OK);
        }
    }

    /**
     * This function generates CMO Sample Id from parameters passed to a query
     *
     * @param correctedCmoSampleViews
     * @return Map of Igo id to CMO Sample Id
     */
    @RequestMapping(name = "/getBulkSampleCmoIdsFromParams", method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> getSampleCmoIdByCmoSampleView(
            @RequestBody CorrectedCmoSampleView[] correctedCmoSampleViews) {

        Map<String, String> cmoSampleIds = new HashMap<>();
        try {
            validate(correctedCmoSampleViews);
            for (CorrectedCmoSampleView correctedCmoSampleView : correctedCmoSampleViews) {
                log.info(String.format("Starting to generate sample cmo id for sample: %s", correctedCmoSampleViews.toString()));

                log.info("Creating Generate sample cmo id task");
                task.init(correctedCmoSampleView);

                log.info("Getting result of Generate sample cmo id task");
                Future<Object> result = connQueue.submitTask(task);

                String correctedSampleCmoId = (String) result.get();
                log.info(String.format("Generated CMO Sample ID: %s", correctedSampleCmoId));

                cmoSampleIds.put(correctedCmoSampleView.getId(), correctedSampleCmoId);
            }
        } catch (Exception e) {
            log.error(String.format("CMO Sample Id error: %s", correctedCmoSampleViews.toString()), e);

            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("ERRORS", e.getLocalizedMessage());

            return new ResponseEntity<>(headers, HttpStatus.OK);
        }

        return ResponseEntity.ok(cmoSampleIds);
    }

    private void validate(CorrectedCmoSampleView[] correctedCmoSampleViews) {
        StringBuilder error = new StringBuilder();
        Multimap<String, String> sampleToErrors = HashMultimap.create();

        for (CorrectedCmoSampleView view : correctedCmoSampleViews) {
            String sampleId = view.getSampleId();
            if (sampleId.isEmpty())
                error.append("Sample id is empty").append(System.lineSeparator());
            else {
                if (view.getSpecimenType() == SpecimenType.CELLLINE) {
                    if (view.getRequestId().isEmpty())
                        sampleToErrors.put(sampleId, "Request id is empty");
                } else {
                    if (view.getPatientId().isEmpty())
                        sampleToErrors.put(sampleId, "Patient id is empty");
                    if (view.getSpecimenType() == null)
                        sampleToErrors.put(sampleId, "Specimen type is empty");
                    // skipping nucleic acid validation since it is not always required
                    // see Confluence "CMO Patient ID and Sample ID generation"
                }
            }
        }

        for (Map.Entry<String, Collection<String>> entry : sampleToErrors.asMap().entrySet()) {
            if (entry.getValue().size() > 0) {
                error.append(entry.getKey() + ":").append(System.lineSeparator());
                for (String e : entry.getValue()) {
                    error.append("" + e).append(", ");
                }
            }
        }

        if (error.length() > 0)
            throw new RuntimeException(error.toString());
    }

    private class IncorrectSampleIgoIdFormatException extends RuntimeException {
        public IncorrectSampleIgoIdFormatException(String message) {
            super(message);
        }
    }
}
