package org.mskcc.limsrest.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.LimsException;
import org.mskcc.limsrest.service.SetOrCreateBanked;
import org.mskcc.limsrest.service.SetOrCreateBankedBatch;
import org.mskcc.limsrest.util.Messages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/")
public class SetBankedSamples {
    private static Log log = LogFactory.getLog(SetBankedSamples.class);
    private final ConnectionPoolLIMS conn;

    public SetBankedSamples(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    public static class BankedSampleRequest {
        @JsonProperty("userId")
        private String userId;
        
        @JsonProperty("user")
        private String user;
        
        @JsonProperty("igoUser")
        private String igoUser;
        
        @JsonProperty("index")
        private String barcodeId;
        
        @JsonProperty("barcodePosition")
        private String barcodePosition;
        
        @JsonProperty("vol")
        private String vol;
        
        @JsonProperty("estimatedPurity")
        private String estimatedPurity;
        
        @JsonProperty("concentration")
        private String concentration;
        
        @JsonProperty("concentrationUnits")
        private String concentrationUnits;
        
        @JsonProperty("sequencingReadLength")
        private String sequencingReadLength;
        
        @JsonProperty("numTubes")
        private String numTubes;
        
        @JsonProperty("assay")
        private String[] assay;
        
        @JsonProperty("clinicalInfo")
        private String clinicalInfo;
        
        @JsonProperty("collectionYear")
        private String collectionYear;
        
        @JsonProperty("gender")
        private String gender;
        
        @JsonProperty("knownGeneticAlteration")
        private String geneticAlterations;
        
        @JsonProperty("rowIndex")
        private String rowIndex;
        
        @JsonProperty("transactionId")
        private String transactionId;
        
        @JsonProperty("organism")
        private String organism;
        
        @JsonProperty("species")
        private String species;
        
        @JsonProperty("preservation")
        private String preservation;
        
        @JsonProperty("specimenType")
        private String specimenType;
        
        @JsonProperty("sampleType")
        private String sampleType;
        
        @JsonProperty("sampleOrigin")
        private String sampleOrigin;
        
        @JsonProperty("micronicTubeBarcode")
        private String micronicTubeBarcode;
        
        @JsonProperty("sampleClass")
        private String sampleClass;
        
        @JsonProperty("requestedReads")
        private String requestedReads;
        
        @JsonProperty("requestedCoverage")
        private String requestedCoverage;
        
        @JsonProperty("spikeInGenes")
        private String spikeInGenes;
        
        @JsonProperty("tissueType")
        private String tissueType;
        
        @JsonProperty("cancerType")
        private String cancerType;
        
        @JsonProperty("recipe")
        private String recipe;
        
        @JsonProperty("capturePanel")
        private String capturePanel;
        
        @JsonProperty("runType")
        private String runType;
        
        @JsonProperty("investigator")
        private String investigator;
        
        @JsonProperty("cellCount")
        private String cellCount;
        
        @JsonProperty("naToExtract")
        private String naToExtract;
        
        @JsonProperty("serviceId")
        private String serviceId;
        
        @JsonProperty("coverage")
        private String coverage;
        
        @JsonProperty("seqRequest")
        private String seqRequest;
        
        @JsonProperty("rowPos")
        private String rowPos;
        
        @JsonProperty("colPos")
        private String colPos;
        
        @JsonProperty("plateId")
        private String plateId;
        
        @JsonProperty("tubeId")
        private String tubeId;
        
        @JsonProperty("patientId")
        private String patientId;
        
        @JsonProperty("normalizedPatientId")
        private String normalizedPatientId;
        
        @JsonProperty("cmoPatientId")
        private String cmoPatientId;
        
        @JsonProperty("numberOfAmplicons")
        private String numberOfAmplicons;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        
        public String getIgoUser() { return igoUser; }
        public void setIgoUser(String igoUser) { this.igoUser = igoUser; }
        
        public String getBarcodeId() { return barcodeId != null ? barcodeId : "NULL"; }
        public void setBarcodeId(String barcodeId) { this.barcodeId = barcodeId; }
        
        public String getBarcodePosition() { return barcodePosition != null ? barcodePosition : "NULL"; }
        public void setBarcodePosition(String barcodePosition) { this.barcodePosition = barcodePosition; }
        
        public String getVol() { return vol != null ? vol : "-1.0"; }
        public void setVol(String vol) { this.vol = vol; }
        
        public String getEstimatedPurity() { return estimatedPurity; }
        public void setEstimatedPurity(String estimatedPurity) { this.estimatedPurity = estimatedPurity; }
        
        public String getConcentration() { return concentration != null ? concentration : "-1.0"; }
        public void setConcentration(String concentration) { this.concentration = concentration; }
        
        public String getConcentrationUnits() { return concentrationUnits != null ? concentrationUnits : "NULL"; }
        public void setConcentrationUnits(String concentrationUnits) { this.concentrationUnits = concentrationUnits; }
        
        public String getSequencingReadLength() { return sequencingReadLength != null ? sequencingReadLength : "NULL"; }
        public void setSequencingReadLength(String sequencingReadLength) { this.sequencingReadLength = sequencingReadLength; }
        
        public String getNumTubes() { return numTubes != null ? numTubes : "NULL"; }
        public void setNumTubes(String numTubes) { this.numTubes = numTubes; }
        
        public String[] getAssay() { return assay; }
        public void setAssay(String[] assay) { this.assay = assay; }
        
        public String getClinicalInfo() { return clinicalInfo != null ? clinicalInfo : "NULL"; }
        public void setClinicalInfo(String clinicalInfo) { this.clinicalInfo = clinicalInfo; }
        
        public String getCollectionYear() { return collectionYear != null ? collectionYear : "NULL"; }
        public void setCollectionYear(String collectionYear) { this.collectionYear = collectionYear; }
        
        public String getGender() { return gender != null ? gender : "NULL"; }
        public void setGender(String gender) { this.gender = gender; }
        
        public String getGeneticAlterations() { return geneticAlterations != null ? geneticAlterations : "NULL"; }
        public void setGeneticAlterations(String geneticAlterations) { this.geneticAlterations = geneticAlterations; }
        
        public String getRowIndex() { return rowIndex; }
        public void setRowIndex(String rowIndex) { this.rowIndex = rowIndex; }
        
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public String getOrganism() { return organism != null ? organism : "NULL"; }
        public void setOrganism(String organism) { this.organism = organism; }
        
        public String getSpecies() { return species != null ? species : "NULL"; }
        public void setSpecies(String species) { this.species = species; }
        
        public String getPreservation() { return preservation != null ? preservation : "NULL"; }
        public void setPreservation(String preservation) { this.preservation = preservation; }
        
        public String getSpecimenType() { return specimenType != null ? specimenType : "NULL"; }
        public void setSpecimenType(String specimenType) { this.specimenType = specimenType; }
        
        public String getSampleType() { return sampleType != null ? sampleType : "NULL"; }
        public void setSampleType(String sampleType) { this.sampleType = sampleType; }
        
        public String getSampleOrigin() { return sampleOrigin != null ? sampleOrigin : "NULL"; }
        public void setSampleOrigin(String sampleOrigin) { this.sampleOrigin = sampleOrigin; }
        
        public String getMicronicTubeBarcode() { return micronicTubeBarcode != null ? micronicTubeBarcode : "NULL"; }
        public void setMicronicTubeBarcode(String micronicTubeBarcode) { this.micronicTubeBarcode = micronicTubeBarcode; }
        
        public String getSampleClass() { return sampleClass != null ? sampleClass : "NULL"; }
        public void setSampleClass(String sampleClass) { this.sampleClass = sampleClass; }
        
        public String getRequestedReads() { return requestedReads; }
        public void setRequestedReads(String requestedReads) { this.requestedReads = requestedReads; }
        
        public String getRequestedCoverage() { return requestedCoverage; }
        public void setRequestedCoverage(String requestedCoverage) { this.requestedCoverage = requestedCoverage; }
        
        public String getSpikeInGenes() { return spikeInGenes != null ? spikeInGenes : "NULL"; }
        public void setSpikeInGenes(String spikeInGenes) { this.spikeInGenes = spikeInGenes; }
        
        public String getTissueType() { return tissueType != null ? tissueType : "NULL"; }
        public void setTissueType(String tissueType) { this.tissueType = tissueType; }
        
        public String getCancerType() { return cancerType != null ? cancerType : ""; }
        public void setCancerType(String cancerType) { this.cancerType = cancerType; }
        
        public String getRecipe() { return recipe != null ? recipe : "NULL"; }
        public void setRecipe(String recipe) { this.recipe = recipe; }
        
        public String getCapturePanel() { return capturePanel != null ? capturePanel : "NULL"; }
        public void setCapturePanel(String capturePanel) { this.capturePanel = capturePanel; }
        
        public String getRunType() { return runType != null ? runType : "NULL"; }
        public void setRunType(String runType) { this.runType = runType; }
        
        public String getInvestigator() { return investigator != null ? investigator : "NULL"; }
        public void setInvestigator(String investigator) { this.investigator = investigator; }
        
        public String getCellCount() { return cellCount != null ? cellCount : "NULL"; }
        public void setCellCount(String cellCount) { this.cellCount = cellCount; }
        
        public String getNaToExtract() { return naToExtract != null ? naToExtract : "NULL"; }
        public void setNaToExtract(String naToExtract) { this.naToExtract = naToExtract; }
        
        public String getServiceId() { return serviceId; }
        public void setServiceId(String serviceId) { this.serviceId = serviceId; }
        
        public String getCoverage() { return coverage != null ? coverage : "NULL"; }
        public void setCoverage(String coverage) { this.coverage = coverage; }
        
        public String getSeqRequest() { return seqRequest != null ? seqRequest : "NULL"; }
        public void setSeqRequest(String seqRequest) { this.seqRequest = seqRequest; }
        
        public String getRowPos() { return rowPos != null ? rowPos : "NULL"; }
        public void setRowPos(String rowPos) { this.rowPos = rowPos; }
        
        public String getColPos() { return colPos != null ? colPos : "NULL"; }
        public void setColPos(String colPos) { this.colPos = colPos; }
        
        public String getPlateId() { return plateId != null ? plateId : "NULL"; }
        public void setPlateId(String plateId) { this.plateId = plateId; }
        
        public String getTubeId() { return tubeId != null ? tubeId : "NULL"; }
        public void setTubeId(String tubeId) { this.tubeId = tubeId; }
        
        public String getPatientId() { return patientId != null ? patientId : "NULL"; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public String getNormalizedPatientId() { return normalizedPatientId != null ? normalizedPatientId : "NULL"; }
        public void setNormalizedPatientId(String normalizedPatientId) { this.normalizedPatientId = normalizedPatientId; }
        
        public String getCmoPatientId() { return cmoPatientId != null ? cmoPatientId : "NULL"; }
        public void setCmoPatientId(String cmoPatientId) { this.cmoPatientId = cmoPatientId; }
        
        public String getNumberOfAmplicons() { return numberOfAmplicons != null ? numberOfAmplicons : "0"; }
        public void setNumberOfAmplicons(String numberOfAmplicons) { this.numberOfAmplicons = numberOfAmplicons; }
    }

    public static class BulkBankedSampleRequest {
        @JsonProperty("samples")
        private List<BankedSampleRequest> samples;
        
        public List<BankedSampleRequest> getSamples() { return samples; }
        public void setSamples(List<BankedSampleRequest> samples) { this.samples = samples; }
    }

    public static class BulkBankedSampleResponse {
        private List<String> results;
        private List<String> errors;
        private int totalProcessed;
        private int totalSuccess;
        private int totalErrors;
        
        public BulkBankedSampleResponse() {
            this.results = new ArrayList<>();
            this.errors = new ArrayList<>();
            this.totalProcessed = 0;
            this.totalSuccess = 0;
            this.totalErrors = 0;
        }
        
        public List<String> getResults() { return results; }
        public void setResults(List<String> results) { this.results = results; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public int getTotalProcessed() { return totalProcessed; }
        public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }
        
        public int getTotalSuccess() { return totalSuccess; }
        public void setTotalSuccess(int totalSuccess) { this.totalSuccess = totalSuccess; }
        
        public int getTotalErrors() { return totalErrors; }
        public void setTotalErrors(int totalErrors) { this.totalErrors = totalErrors; }
    }

    @RequestMapping(value = "/setBankedSamples", method = RequestMethod.POST)
    public ResponseEntity<BulkBankedSampleResponse> setBankedSamples(@RequestBody BulkBankedSampleRequest request) {
        log.info("Starting bulk banked sample operation for " + request.getSamples().size() + " samples");
        
        BulkBankedSampleResponse response = new BulkBankedSampleResponse();
        
        if (request.getSamples() == null || request.getSamples().isEmpty()) {
            response.getErrors().add("No samples provided in request");
            response.setTotalErrors(1);
            return ResponseEntity.badRequest().body(response);
        }
        
        List<Future<Object>> futures = new ArrayList<>();
        
        // Process each sample
        // Validate all samples first and collect valid ones
        List<BankedSampleRequest> validSamples = new ArrayList<>();
        int numSamples = (request.getSamples() != null) ? request.getSamples().size() : 0;
        for (BankedSampleRequest sampleRequest : request.getSamples()) {
            boolean valid = true;
            if (sampleRequest.getUserId() == null || sampleRequest.getUserId().trim().isEmpty()) {
                response.getErrors().add("Sample missing required field: userId");
                response.setTotalErrors(response.getTotalErrors() + 1);
                valid = false;
            }
            if (sampleRequest.getServiceId() == null || sampleRequest.getServiceId().trim().isEmpty()) {
                response.getErrors().add("Sample " + sampleRequest.getUserId() + " missing required field: serviceId");
                response.setTotalErrors(response.getTotalErrors() + 1);
                valid = false;
            }
            if (sampleRequest.getServiceId() != null && !Whitelists.textMatches(sampleRequest.getServiceId())) {
                response.getErrors().add("Sample " + sampleRequest.getUserId() + " has invalid serviceId format");
                response.setTotalErrors(response.getTotalErrors() + 1);
                valid = false;
            }
            if (sampleRequest.getUserId() != null && !Whitelists.sampleMatches(sampleRequest.getUserId())) {
                response.getErrors().add("Sample " + sampleRequest.getUserId() + " has invalid userId format");
                response.setTotalErrors(response.getTotalErrors() + 1);
                valid = false;
            }
            if (valid) {
                // Set default values for assay if null
                if (sampleRequest.getAssay() == null) {
                    sampleRequest.setAssay(new String[] { "NULL" });
                }
                // Set species default
                if (sampleRequest.getSpecies() == null || "NULL".equals(sampleRequest.getSpecies())) {
                    sampleRequest.setSpecies(sampleRequest.getOrganism());
                }
                // Parse estimated purity
                if (sampleRequest.getEstimatedPurity() != null) {
                    try {
                        Double.parseDouble(sampleRequest.getEstimatedPurity());
                    } catch (NumberFormatException nfe) {
                        sampleRequest.setEstimatedPurity("0.0");
                    }
                }
                validSamples.add(sampleRequest);
            }
        }

        if (!validSamples.isEmpty()) {
            try {
                // Submit all valid samples in one batch task
                SetOrCreateBankedBatch batchTask = new SetOrCreateBankedBatch(validSamples, validSamples.get(0).getIgoUser());
                Future<Object> future = conn.submitTask(batchTask);
                futures.add(future);
            } catch (Exception e) {
                log.error("Error submitting batch task: " + e.getMessage(), e);
                response.getErrors().add("Batch processing error: " + e.getMessage());
                response.setTotalErrors(response.getTotalErrors() + validSamples.size());
            }
        }
        
        // Collect results
        for (int i = 0; i < futures.size(); i++) {
            try {
                Object result = futures.get(i).get();
                if (result instanceof List) {
                    // Batch result - process each item in the list
                    List<String> batchResults = (List<String>) result;
                    for (String returnCode : batchResults) {
                        if (returnCode.startsWith(Messages.ERROR_IN)) {
                            response.getErrors().add(returnCode);
                            response.setTotalErrors(response.getTotalErrors() + 1);
                        } else {
                            response.getResults().add("Record Id:" + returnCode);
                            response.setTotalSuccess(response.getTotalSuccess() + 1);
                        }
                    }
                } else {
                    // Single result (fallback)
                    String returnCode = (String) result;
                    if (returnCode.startsWith(Messages.ERROR_IN)) {
                        throw new LimsException(returnCode);
                    }
                    response.getResults().add("Record Id:" + returnCode);
                    response.setTotalSuccess(response.getTotalSuccess() + 1);
                }
            } catch (Exception e) {
                log.error("Error getting result for batch " + i + ": " + e.getMessage(), e);
                response.getErrors().add("Batch " + i + " result error: " + e.getMessage());
                response.setTotalErrors(response.getTotalErrors() + 1);
            }
        }
        
        response.setTotalProcessed(request.getSamples().size());
        
        log.info("Bulk operation completed. Processed: " + response.getTotalProcessed() + 
                ", Success: " + response.getTotalSuccess() + ", Errors: " + response.getTotalErrors());
        
        return ResponseEntity.ok(response);
    }
}
