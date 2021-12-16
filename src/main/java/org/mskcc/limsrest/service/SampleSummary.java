package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedList;
@Setter @ToString
public class SampleSummary {
    private String id;
    private String assay;
    private String barcodeId;
    private String cellCount;
    private String clinicalInfo;
    private String cmoId; // OtherSampleId
    private String cmoPatientId;
    private String collectionYear;
    private String colPosition;
    private double concentration;
    private String concentrationUnits;
    private String correctedCmoId;  // missing
    private Integer coverage; // missing
    private long dropOffDate;
    private double estimatedPurity;
    private String gender;
    private String geneticAlterations;
    private String initialPool;  // missing
    private String investigator;
    private String micronicTubeBarcode;
    private String naToExtract;
    private String normalizedPatientId;
    private String numTubes;
    private String organism;
    private String patientId;
    private String plateId;
    private String platform;
    private String preservation;
    private String project;  // request
    private String recipe;
    private Long readNumber;  // missing
    private String readSummary; // missing
    private String requestedReads;
    private String requestedCoverage;
    private Long recordId;
    private String rowPosition;
    private String runType;
    private String sampleClass;
    private String sampleType;
    private String serviceId;
    private String specimenType;
    private String spikeInGenes;
    private String tissueType; // TissueSite
    private String tubeId;  // TubeBarcode
    private String tumorType;
    private String tumorOrNormal;
    private String userId; // expName
    private double volume;
    private double yield;  // missing
    private Integer rowIndex;
    private Long transactionId;
    private String capturePanel;


    private SampleQcSummary qc;
    private LinkedList<BasicQc> basicQcs;

    public SampleSummary() {
        id = "ERROR";
        userId = "";
        project = "";
        recipe = "";
        organism = "";
        yield = 0;
        serviceId = "";
    }

    public void addBasicQc(BasicQc qc) {
        if (basicQcs == null) {
            basicQcs = new LinkedList<>();
        }
        basicQcs.add(qc);
    }

    public void addExpName(String userId) { this.userId = userId; }

    public void addBaseId(String id) {
        this.id = id;
    }

    public void addCmoId(String otherId) {
        this.cmoId = otherId;
    }

    public void addConcentration(double concentration) {
        this.concentration = concentration;
    }

    public void addConcentrationUnits(String units) {
        this.concentrationUnits = units;
    }

    public void addRequest(String proj) { this.project = proj; }

    public void addVolume(double volume) {
        this.volume = volume;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getBaseId() {
        return id;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getCmoId() {
        return cmoId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getConcentration() {
        return concentration + " " + concentrationUnits;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getVolume() {
        return volume;
    }

    //Duplicate
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getVol() {
        return volume;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getEstimatedPurity() {
        return estimatedPurity;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getYield() {
        return yield;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getNumTubes() {
        return numTubes;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getNaToExtract() {
        return naToExtract;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPatientId() {
        return patientId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPlatform() {
        return platform;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getAssay() {
        return assay;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getBarcodeId() {
        return barcodeId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getGeneticAlterations() {
        return geneticAlterations;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getKnownGeneticAlteration() {
        return geneticAlterations;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getGender() {
        return gender;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getInitialPool() {
        return initialPool;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getInvestigator() {
        return investigator;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getMicronicTubeBarcode() {
        return micronicTubeBarcode;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPreservation() {
        return preservation;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleClass() {
        return sampleClass;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleType() {
        return sampleType;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getServiceId() {
        return serviceId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSpecimenType() {
        return specimenType;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSpikeInGenes() {
        return spikeInGenes;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getTissueSite() {
        return tissueType;
    }

    //Duplicate
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getTissueType() {
        return tissueType;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getTubeId() {
        return tubeId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getCellCount() {
        return cellCount;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getClinicalInfo() {
        return clinicalInfo;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getCollectionYear() {
        return collectionYear;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getCorrectedCmoId() {
        return correctedCmoId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPlateId() {
        return plateId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getColPosition() {
        return colPosition;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRowPosition() {
        return rowPosition;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRecipe() {
        return recipe;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getReadSummary() {
        return readSummary;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRequestedReads() {
        return requestedReads;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRequestedCoverage() {
        return requestedCoverage;
    }

    public String getCancerType() {
        if (tumorType == null) {
            return tumorOrNormal;
        }
        return tumorType;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getTumorOrNormal() {
        return tumorOrNormal;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getProject() {
        return project;
    }

    public String getExpName() {
        return userId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getUserId() {
        return userId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public SampleQcSummary getQc() {
        return qc;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public LinkedList<BasicQc> getBasicQcs() {
        return basicQcs;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSpecies() {
        return organism;
    }

    //duplicate
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getOrganism() {
        return organism;
    }

    public long getDropOffDate() {
        return dropOffDate;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getRecordId() {
        return recordId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getRequestedNumberOfReads() {
        return readNumber;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Integer getCoverageTarget() {
        return coverage;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRunType() {
        return runType;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getNormalizedPatientId() {
        return normalizedPatientId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getCmoPatientId() {
        return cmoPatientId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Integer getRowIndex() {
        return rowIndex;
    }
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getTransactionId() {
        return transactionId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getCapturePanel() {
        return capturePanel;
    }
}