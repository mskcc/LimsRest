package org.mskcc.limsrest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.mskcc.limsrest.service.CMOSampleIdFields;

import java.io.Serializable;
import java.util.*;

/**
 * Sample Level information which describe metadata and wetlab information reported to pipelines.
 * <BR>
 * Samples can have libraries, libraries have sequencer runs, runs have fastqs.
 */
@JsonIgnoreProperties(value = { "cmoInfoIgoId" })
@Getter @Setter
@ToString @NoArgsConstructor
public class SampleManifest implements Serializable {
    private String igoId;
    private String altid; // added in Dec. 2024 by request of the CMO
    private String cmoInfoIgoId;
    private String cmoSampleName;
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
    private String estimatedPurity;

    protected List<QcReport> qcReports = new ArrayList<>();
    protected Set<Library> libraries = new HashSet<>();

    public enum QcReportType {
        DNA, RNA, LIBRARY;
    }

    // fields added so the Meta DB team can build CMO sample IDs outside the IGO LIMS
    private CMOSampleIdFields cmoSampleIdFields = new CMOSampleIdFields();

    public void addLibrary(Library library) {
        libraries.add(library);
    }

    public void addQcReport(QcReport r) {
        qcReports.add(r);
    }

    /*
     * The IGO qc reports (DNA, RNA, Library) are returned with pipeline data and meant for the analyst reviewing
     * the data to better interpret the pipeline results.
     */
    public static class QcReport {
        public QcReport() {}

        public QcReport(QcReportType qcReportType, 
        String IGORecommendation, String comments, String investigatorDecision, String DIN) {
            this.qcReportType = qcReportType;
            this.IGORecommendation = IGORecommendation;
            this.comments = comments;
            this.investigatorDecision = investigatorDecision;
            this.din = DIN;
        }

        public QcReportType qcReportType;
        public String IGORecommendation;
        public String comments;
        public String investigatorDecision;
        public String din;
    }
    @NoArgsConstructor @ToString
    @Getter @Setter
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

        public void addRun(Run r) {
            runs.add(r);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Library library = (Library) o;

            if (barcodeId != null ? !barcodeId.equals(library.barcodeId) : library.barcodeId != null) return false;
            if (barcodeIndex != null ? !barcodeIndex.equals(library.barcodeIndex) : library.barcodeIndex != null)
                return false;
            return libraryIgoId != null ? libraryIgoId.equals(library.libraryIgoId) : library.libraryIgoId == null;
        }

        @Override
        public int hashCode() {
            int result = barcodeId != null ? barcodeId.hashCode() : 0;
            result = 31 * result + (barcodeIndex != null ? barcodeIndex.hashCode() : 0);
            result = 31 * result + (libraryIgoId != null ? libraryIgoId.hashCode() : 0);
            return result;
        }
    }

    @ToString
    @Getter @Setter
    public static class Run {
        private String runMode;
        private String runId;
        private String flowCellId;
        private String readLength;
        private String runDate;

        private List<Integer> flowCellLanes = new ArrayList<>();
        private List<String> fastqs = new ArrayList<>();

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

        public void addFastq(String fastq) {
            fastqs.add(fastq);
        }
    }
}