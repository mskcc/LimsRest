package org.mskcc.limsrest.limsapi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedList;

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

    public void setRequestedReadNumber(long readNum) {
        this.readNumber = readNum;
    }

    public void setTumorType(String tumorType) {
        this.tumorType = tumorType;
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
        return Double.toString(concentration) + " " + concentrationUnits;
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

    public void setEstimatedPurity(double ep) {
        this.estimatedPurity = ep;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getYield() {
        return yield;
    }

    public void setYield(double yield) {
        this.yield = yield;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getNumTubes() {
        return numTubes;
    }

    public void setNumTubes(String numTubes) {
        this.numTubes = numTubes;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getNaToExtract() {
        return naToExtract;
    }

    public void setNaToExtract(String naToExtract) {
        this.naToExtract = naToExtract;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getAssay() {
        return assay;
    }

    public void setAssay(String assay) {
        this.assay = assay;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getBarcodeId() {
        return barcodeId;
    }

    public void setBarcodeId(String barcodeId) { this.barcodeId = barcodeId; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getGeneticAlterations() {
        return geneticAlterations;
    }

    public void setGeneticAlterations(String geneticAlterations) {
        this.geneticAlterations = geneticAlterations;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getKnownGeneticAlteration() {
        return geneticAlterations;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getInitialPool() {
        return initialPool;
    }

    public void setInitialPool(String pool) {
        this.initialPool = pool;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getInvestigator() {
        return investigator;
    }

    public void setInvestigator(String investigator) {
        this.investigator = investigator;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getMicronicTubeBarcode() {
        return micronicTubeBarcode;
    }

    public void setMicronicTubeBarcode(String micronic) {
        this.micronicTubeBarcode = micronic;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPreservation() {
        return preservation;
    }

    public void setPreservation(String preservation) {
        this.preservation = preservation;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleClass() {
        return sampleClass;
    }

    public void setSampleClass(String sampleClass) {
        this.sampleClass = sampleClass;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSpecimenType() {
        return specimenType;
    }

    public void setSpecimenType(String specimenType) {
        this.specimenType = specimenType;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSpikeInGenes() {
        return spikeInGenes;
    }

    public void setSpikeInGenes(String spikeInGenes) {
        this.spikeInGenes = spikeInGenes;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getTissueSite() {
        return tissueType;
    }

    public void setTissueSite(String tissueType) {
        this.tissueType = tissueType;
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

    public void setTubeId(String tubeId) {
        this.tubeId = tubeId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getCellCount() {
        return cellCount;
    }

    public void setCellCount(String cc) {
        this.cellCount = cc;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getClinicalInfo() {
        return clinicalInfo;
    }

    public void setClinicalInfo(String ci) {
        this.clinicalInfo = ci;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getCollectionYear() {
        return collectionYear;
    }

    public void setCollectionYear(String year) {
        this.collectionYear = year;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getCorrectedCmoId() {
        return correctedCmoId;
    }

    public void setCorrectedCmoId(String correctedCmoId) {
        this.correctedCmoId = correctedCmoId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPlateId() {
        return plateId;
    }

    public void setPlateId(String plateId) {
        this.plateId = plateId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getColPosition() {
        return colPosition;
    }

    public void setColPosition(String colPos) {
        this.colPosition = colPos;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRowPosition() {
        return rowPosition;
    }

    public void setRowPosition(String rowPos) {
        this.rowPosition = rowPos;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) { this.recipe = recipe; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getReadSummary() {
        return readSummary;
    }

    public void setReadSummary(String readSummary) {
        this.readSummary = readSummary;
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

    public void setTumorOrNormal(String tumorOrNormal) {
        this.tumorOrNormal = tumorOrNormal;
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

    public void setQc(SampleQcSummary qc) {
        this.qc = qc;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public LinkedList<BasicQc> getBasicQcs() {
        return basicQcs;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSpecies() {
        return organism;
    }

    public void setSpecies(String organism) {
        this.organism = organism;
    }

    //duplicate
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getOrganism() {
        return organism;
    }

    public long getDropOffDate() {
        return dropOffDate;
    }

    public void setDropOffDate(long date) {
        this.dropOffDate = date;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(long recordId) {
        this.recordId = Long.valueOf(recordId);
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getRequestedNumberOfReads() {
        return readNumber;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Integer getCoverageTarget() {
        return coverage;
    }

    public void setCoverageTarget(int coverage) {
        this.coverage = coverage;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRunType() {
        return runType;
    }

    public void setRunType(String runType) {
        this.runType = runType;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getNormalizedPatientId() {
        return normalizedPatientId;
    }

    public void setNormalizedPatientId(String normalizedPatientId) {
        this.normalizedPatientId = normalizedPatientId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getCmoPatientId() {
        return cmoPatientId;
    }

    public void setCmoPatientId(String cmoPatientId) {
        this.cmoPatientId = cmoPatientId;
    }
    @Override
    public String toString() {
        return "SampleSummary{" +
                "id='" + id + '\'' +
                ", assay='" + assay + '\'' +
                ", barcodeId='" + barcodeId + '\'' +
                ", cellCount='" + cellCount + '\'' +
                ", clinicalInfo='" + clinicalInfo + '\'' +
                ", cmoId='" + cmoId + '\'' +
                ", cmoPatientId='" + cmoPatientId + '\'' +
                ", collectionYear='" + collectionYear + '\'' +
                ", colPosition='" + colPosition + '\'' +
                ", concentration=" + concentration +
                ", concentrationUnits='" + concentrationUnits + '\'' +
                ", correctedCmoId='" + correctedCmoId + '\'' +
                ", coverage=" + coverage +
                ", dropOffDate=" + dropOffDate +
                ", estimatedPurity=" + estimatedPurity +
                ", gender='" + gender + '\'' +
                ", geneticAlterations='" + geneticAlterations + '\'' +
                ", initialPool='" + initialPool + '\'' +
                ", investigator='" + investigator + '\'' +
                ", micronicTubeBarcode='" + micronicTubeBarcode + '\'' +
                ", naToExtract='" + naToExtract + '\'' +
                ", normalizedPatientId='" + normalizedPatientId + '\'' +
                ", numTubes='" + numTubes + '\'' +
                ", organism='" + organism + '\'' +
                ", patientId='" + patientId + '\'' +
                ", plateId='" + plateId + '\'' +
                ", platform='" + platform + '\'' +
                ", preservation='" + preservation + '\'' +
                ", project='" + project + '\'' +
                ", recipe='" + recipe + '\'' +
                ", readNumber=" + readNumber +
                ", readSummary='" + readSummary + '\'' +
                ", recordId=" + recordId +
                ", rowPosition='" + rowPosition + '\'' +
                ", runType='" + runType + '\'' +
                ", sampleClass='" + sampleClass + '\'' +
                ", sampleType='" + sampleType + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", specimenType='" + specimenType + '\'' +
                ", spikeInGenes='" + spikeInGenes + '\'' +
                ", tissueType='" + tissueType + '\'' +
                ", tubeId='" + tubeId + '\'' +
                ", tumorType='" + tumorType + '\'' +
                ", tumorOrNormal='" + tumorOrNormal + '\'' +
                ", userId='" + userId + '\'' +
                ", volume=" + volume +
                ", yield=" + yield +
                ", qc=" + qc +
                ", basicQcs=" + basicQcs +
                '}';
    }
}