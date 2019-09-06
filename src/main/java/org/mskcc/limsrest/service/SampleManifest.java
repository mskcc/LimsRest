package org.mskcc.limsrest.service;

import java.util.ArrayList;
import java.util.List;

public class SampleManifest {
    private String igoId;

    private String cmoSampleId;
    private String cmoPatientId;
    private String investigatorSampleId;
    private String oncoTreeCode;
    private String tumorOrNormal;
    private String tissueLocation;
    private String sampleOrigin;
    private String preservation;
    private String collectionYear;
    private String gender;
    private String species;

    private String baitSet;

    private List<Library> libraries = new ArrayList<>();

    public static class Library {
        public String barcodeId;
        public String barcodeIndex;

        public String libaryIgoId;
        public Double libraryVolume; // [uL]
        public Double libraryConcentrationNgul; // ng/uL

        public String captureConcentrationNm;
        public String captureInputNg;
        public String captureName;

        public List<Run> runs = new ArrayList<>();

        public Library(String libaryIgoId, Double libraryVolume, Double libraryConcentrationNgul) {
            this.libaryIgoId = libaryIgoId;
            this.libraryVolume = libraryVolume;
            this.libraryConcentrationNgul = libraryConcentrationNgul;
        }
    }

    public static class Run {
        public Run(String runMode, String runId, String flowCellId, Integer flowCelllane, String readLength, String runDate, List<String> fastqs) {
            this.runMode = runMode;
            this.runId = runId;
            this.flowCellId = flowCellId;
            this.flowCellLane = flowCelllane;
            this.readLength = readLength;
            this.runDate = runDate;
            this.fastqs = fastqs;
        }
        public String runMode;
        public String runId;
        public String flowCellId;
        public Integer flowCellLane;
        public String readLength;
        public String runDate;
        public List<String> fastqs;
    }

    public SampleManifest() {}

    public String getCmoSampleId() { return cmoSampleId; }

    public void setCmoSampleId(String cmoSampleId) { this.cmoSampleId = cmoSampleId; }

    public String getSpecies() { return species; }

    public void setSpecies(String species) { this.species = species; }

    public String getTumorOrNormal() {
        return tumorOrNormal;
    }

    public void setTumorOrNormal(String tumorOrNormal) {
        this.tumorOrNormal = tumorOrNormal;
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

    public void setInvestigatorSampleId(String investigatorSampleId) { this.investigatorSampleId = investigatorSampleId; }

    public String getOncoTreeCode() {
        return oncoTreeCode;
    }

    public void setOncoTreeCode(String oncoTreeCode) {
        this.oncoTreeCode = oncoTreeCode;
    }

    public String getTissueLocation() {
        return tissueLocation;
    }

    public void setTissueLocation(String tissueLocation) {
        this.tissueLocation = tissueLocation;
    }

    public String getSampleOrigin() {
        return sampleOrigin;
    }

    public void setSampleOrigin(String sampleOrigin) {
        this.sampleOrigin = sampleOrigin;
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

    public String getBaitSet() {
        return baitSet;
    }

    public void setBaitSet(String baitSet) {
        this.baitSet = baitSet;
    }

    public List<Library> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<Library> libraries) {
        this.libraries = libraries;
    }
}