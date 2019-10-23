package org.mskcc.limsrest.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SampleManifest {
    private String igoId;

    private String cmoSampleName; // aka "Corrected CMO Sample ID", but not an ID in by normal database standards
    private String cmoPatientId;
    private String investigatorSampleId;
    private String oncoTreeCode;
    private String tumorOrNormal;
    private String tissueLocation;
    private String sampleOrigin;
    private String preservation;
    private String collectionYear;
    private String sex;
    private String species;

    private String baitSet;

    private List<Library> libraries = new ArrayList<>();

    public static class Library {
        public String barcodeId;
        public String barcodeIndex;

        public String libraryIgoId;
        public Double libraryVolume; // [uL]
        public Double libraryConcentrationNgul; // ng/uL

        public String captureConcentrationNm;
        public String captureInputNg;
        public String captureName;

        public List<Run> runs = new ArrayList<>();

        public Library(String libraryIgoId, Double libraryVolume, Double libraryConcentrationNgul) {
            this.libraryIgoId = libraryIgoId;
            this.libraryVolume = libraryVolume;
            this.libraryConcentrationNgul = libraryConcentrationNgul;
        }

        public boolean hasFastqs() {
            for (Run run : runs) {
                if (run.fastqs != null)
                    return true;
            }
            return false;
        }

        public Integer nFastqs() {
            Integer fastqs = 0;
            for (Run run : runs) {
                if (run.fastqs != null)
                    fastqs += run.fastqs.size();
            }
            return fastqs;
        }

        @Override
        public String toString() {
            return "Library{" +
                    "barcodeId='" + barcodeId + '\'' +
                    ", barcodeIndex='" + barcodeIndex + '\'' +
                    ", libraryIgoId='" + libraryIgoId + '\'' +
                    ", libraryVolume=" + libraryVolume +
                    ", libraryConcentrationNgul=" + libraryConcentrationNgul +
                    ", captureConcentrationNm='" + captureConcentrationNm + '\'' +
                    ", captureInputNg='" + captureInputNg + '\'' +
                    ", captureName='" + captureName + '\'' +
                    ", runs=" + runs +
                    '}';
        }
    }

    public static class Run {
        public String runMode;
        public String runId;
        public String flowCellId;
        public String readLength;
        public String runDate;

        public List<Integer> flowCellLanes = new ArrayList<>();
        public List<String> fastqs;

        public Run(String runMode, String runId, String flowCellId, String readLength, String runDate) {
            this.runMode = runMode;
            this.runId = runId;
            this.flowCellId = flowCellId;
            this.readLength = readLength;
            this.runDate = runDate;
            this.fastqs = fastqs;
        }

        public void addLane(Integer lane) {
            flowCellLanes.add(lane);
            Collections.sort(flowCellLanes);
        }

        @Override
        public String toString() {
            return "Run{" +
                    "runMode='" + runMode + '\'' +
                    ", runId='" + runId + '\'' +
                    ", flowCellId='" + flowCellId + '\'' +
                    ", readLength='" + readLength + '\'' +
                    ", runDate='" + runDate + '\'' +
                    ", flowCellLanes=" + flowCellLanes +
                    ", fastqs=" + fastqs +
                    '}';
        }
    }

    public SampleManifest() {}

    public String getCmoSampleName() { return cmoSampleName; }

    public void setCmoSampleName(String cmoSampleName) { this.cmoSampleName = cmoSampleName; }

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

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
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

    @Override
    public String toString() {
        return "SampleManifest{" +
                "igoId='" + igoId + '\'' +
                ", cmoSampleName='" + cmoSampleName + '\'' +
                ", cmoPatientId='" + cmoPatientId + '\'' +
                ", investigatorSampleId='" + investigatorSampleId + '\'' +
                ", oncoTreeCode='" + oncoTreeCode + '\'' +
                ", tumorOrNormal='" + tumorOrNormal + '\'' +
                ", tissueLocation='" + tissueLocation + '\'' +
                ", sampleOrigin='" + sampleOrigin + '\'' +
                ", preservation='" + preservation + '\'' +
                ", collectionYear='" + collectionYear + '\'' +
                ", sex='" + sex + '\'' +
                ", species='" + species + '\'' +
                ", baitSet='" + baitSet + '\'' +
                ", libraries=" + libraries +
                '}';
    }
}