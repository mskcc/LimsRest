package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sample Level information which describe metadata and wetlab information reported to pipelines.
 * <BR>
 * Samples can have libraries, libraries can have runs, runs have fastqs.
 */
@JsonIgnoreProperties(value = { "cmoInfoIgoId" })
public class SampleManifest {
    private String igoId;

    public String cmoInfoIgoId; // left out of pipeline JSON

    private String cmoSampleName; // aka "Corrected CMO Sample ID", but not an ID in by normal database standards
    private String sampleName;
    private String cmoSampleClass;
    private String cmoPatientId;
    private String investigatorSampleId;
    private String oncoTreeCode;
    private String tumorOrNormal;
    private String tissueLocation;
    private String specimenType;
    private String sampleOrigin;
    private String preservation;
    private String collectionYear;
    private String sex;
    private String species;
    private String tubeId;
    private String cfDNA2dBarcode;

    private String baitSet;

    private List<QcReport> qcReports = new ArrayList<>();

    private List<Library> libraries = new ArrayList<>();

    public enum QcReportType {
        DNA, RNA, LIBRARY;
    }

    /*
     * The IGO qc reports (DNA, RNA, Library) are returned with pipeline data and meant for the analyst reviewing
     * the data to better interpret the pipeline results.
     */
    public static class QcReport {
        public QcReport() {}

        public QcReport(QcReportType qcReportType, String IGORecommendation, String comments, String investigatorDecision) {
            this.qcReportType = qcReportType;
            this.IGORecommendation = IGORecommendation;
            this.comments = comments;
            this.investigatorDecision = investigatorDecision;
        }

        public QcReportType qcReportType;
        public String IGORecommendation;
        public String comments;
        public String investigatorDecision;
    }

    public static class Library {
        public String barcodeId;
        public String barcodeIndex;

        public String libraryIgoId;
        public Double libraryVolume; // [uL]
        public Double libraryConcentrationNgul; // ng/uL

        public Double dnaInputNg;

        public String captureConcentrationNm;
        public String captureInputNg;
        public String captureName;

        public List<Run> runs = new ArrayList<>();

        public Library() {}

        public Library(String libraryIgoId, Double libraryVolume, Double libraryConcentrationNgul, Double dnaInputNg) {
            this.libraryIgoId = libraryIgoId;
            this.libraryVolume = libraryVolume;
            this.libraryConcentrationNgul = libraryConcentrationNgul;
            this.dnaInputNg = dnaInputNg;
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
                    ", dnaInputNg=" + dnaInputNg +
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

        public Run(String runId, String flowCellId, String runDate) {
            this.runId = runId;
            this.flowCellId = flowCellId;
            this.runDate = runDate;
        }

        public Run(String runMode, String runId, String flowCellId, String readLength, String runDate) {
            this.runMode = runMode;
            this.runId = runId;
            this.flowCellId = flowCellId;
            this.readLength = readLength;
            this.runDate = runDate;
        }

        public Run(List<String> fastqs) {
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

    public String getTubeId() { return tubeId; }

    public void setTubeId(String tubeId) { this.tubeId = tubeId; }

    public String getCfDNA2dBarcode() { return cfDNA2dBarcode; }

    public void setCfDNA2dBarcode(String cfDNA2dBarcode) { this.cfDNA2dBarcode = cfDNA2dBarcode; }

    public List<QcReport> getQcReports() { return qcReports; }

    public void setQcReports(List<QcReport> qcReports) { this.qcReports = qcReports; }

    public void setCMOSampleClass(String cmoSampleClass) { this.cmoSampleClass = cmoSampleClass; }

    public String getCmoSampleClass() { return cmoSampleClass; }

    public void setSampleName(String sampleName) { this.sampleName = sampleName; }

    public String getSampleName () { return sampleName; }

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

    public String getSpecimenType() { return specimenType; }

    public void setSpecimenType(String specimenType) { this.specimenType = specimenType; }

    public String getTissueLocation() {
        return tissueLocation;
    }

    public void setTissueLocation(String tissueLocation) {
        this.tissueLocation = tissueLocation;
    }

    public String getSampleOrigin() {
        return sampleOrigin;
    }

    public void setSampleOrigin(String sampleOrigin) { this.sampleOrigin = sampleOrigin; }

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

    public String getBaitSet() { return baitSet; }

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
                ", cmoInfoIgoId='" + cmoInfoIgoId + '\'' +
                ", cmoSampleName='" + cmoSampleName + '\'' +
                ", sampleName='" + sampleName + '\'' +
                ", cmoSampleClass='" + cmoSampleClass + '\'' +
                ", cmoPatientId='" + cmoPatientId + '\'' +
                ", investigatorSampleId='" + investigatorSampleId + '\'' +
                ", oncoTreeCode='" + oncoTreeCode + '\'' +
                ", tumorOrNormal='" + tumorOrNormal + '\'' +
                ", tissueLocation='" + tissueLocation + '\'' +
                ", specimenType='" + specimenType + '\'' +
                ", sampleOrigin='" + sampleOrigin + '\'' +
                ", preservation='" + preservation + '\'' +
                ", collectionYear='" + collectionYear + '\'' +
                ", sex='" + sex + '\'' +
                ", species='" + species + '\'' +
                ", tubeId='" + tubeId + '\'' +
                ", cfDNA2dBarcode='" + cfDNA2dBarcode + '\'' +
                ", baitSet='" + baitSet + '\'' +
                ", qcReports=" + qcReports +
                ", libraries=" + libraries +
                '}';
    }
}