package org.mskcc.limsrest.limsapi;

public class SampleManifest {
    private String igoId;
    private String cmoPatientId;
    private String investigatorSampleId;
    private String oncotreeCode;
    private String sampleClass;
    private String tissueSite;
    private String sampleType;
    private String preservation;
    private String collectionYear;
    private String gender;
    private String recipe;
    private String barcodeId;
    private String barcodeIndex;

    private Double libraryInputNg; // [ng]
    private Double libraryConcentration; // ng/uL
    private Double libraryYieldNg;
    private String captureInputNg;
    private String captureName;
    private String captureConcentrationNm;

    private String runId;
    private String laneNumber;


    public SampleManifest() {}

    public Double getLibraryConcentration() {
        return libraryConcentration;
    }

    public void setLibraryConcentration(Double libraryConcentration) {
        this.libraryConcentration = libraryConcentration;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public String getIgoId() {
        return igoId;
    }

    public void setIgoId(String igoId) {
        this.igoId = igoId;
    }

    public String getCmoPatientId() {
        return cmoPatientId;
    }

    public void setCmoPatientId(String cmoPatientId) {
        this.cmoPatientId = cmoPatientId;
    }

    public String getInvestigatorSampleId() {
        return investigatorSampleId;
    }

    public void setInvestigatorSampleId(String investigatorSampleId) {
        this.investigatorSampleId = investigatorSampleId;
    }

    public String getOncotreeCode() {
        return oncotreeCode;
    }

    public void setOncotreeCode(String oncotreeCode) {
        this.oncotreeCode = oncotreeCode;
    }

    public String getSampleClass() {
        return sampleClass;
    }

    public void setSampleClass(String sampleClass) {
        this.sampleClass = sampleClass;
    }

    public String getTissueSite() {
        return tissueSite;
    }

    public void setTissueSite(String tissueSite) {
        this.tissueSite = tissueSite;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getPreservation() {
        return preservation;
    }

    public void setPreservation(String preservation) {
        this.preservation = preservation;
    }

    public String getCollectionYear() {
        return collectionYear;
    }

    public void setCollectionYear(String collectionYear) {
        this.collectionYear = collectionYear;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBarcodeId() {
        return barcodeId;
    }

    public void setBarcodeId(String barcodeId) {
        this.barcodeId = barcodeId;
    }

    public String getBarcodeIndex() {
        return barcodeIndex;
    }

    public void setBarcodeIndex(String barcodeIndex) {
        this.barcodeIndex = barcodeIndex;
    }

    public Double getLibraryInputNg() {
        return libraryInputNg;
    }

    public void setLibraryInputNg(Double libraryInputNg) {
        this.libraryInputNg = libraryInputNg;
    }

    public Double getLibraryYieldNg() {
        return libraryYieldNg;
    }

    public void setLibraryYieldNg(Double libraryYieldNg) {
        this.libraryYieldNg = libraryYieldNg;
    }

    public String getCaptureInputNg() {
        return captureInputNg;
    }

    public void setCaptureInputNg(String captureInputNg) {
        this.captureInputNg = captureInputNg;
    }

    public String getCaptureName() {
        return captureName;
    }

    public void setCaptureName(String captureName) {
        this.captureName = captureName;
    }

    public String getCaptureConcentrationNm() {
        return captureConcentrationNm;
    }

    public void setCaptureConcentrationNm(String captureConcentrationNm) {
        this.captureConcentrationNm = captureConcentrationNm;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getLaneNumber() {
        return laneNumber;
    }

    public void setLaneNumber(String laneNumber) {
        this.laneNumber = laneNumber;
    }
}