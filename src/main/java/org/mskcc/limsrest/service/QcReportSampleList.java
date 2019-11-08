package org.mskcc.limsrest.service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

// base class for DNA/RNA/Library Report Samples
public class QcReportSampleList {
    public String requestId;
    public List<Object> requestSampleIds;
    List<ReportSample> dnaReportSamples;
    List<ReportSample> rnaReportSamples;
    List<ReportSample> libraryReportSamples;
    List<PathologySample> pathologyReportSamples;
    List<HashMap<String, Object>> attachments;

    public QcReportSampleList() {
    }

    public QcReportSampleList(String requestId, List<Object> requestSampleIds) {
        this.requestId = requestId;
        this.requestSampleIds = requestSampleIds;
        dnaReportSamples = null;
        rnaReportSamples = null;
        libraryReportSamples = null;
        pathologyReportSamples = null;
        attachments = null;
    }

    public QcReportSampleList(String requestId) {
        this.requestId = requestId;
    }

    public List<Object> getRequestSampleIds() {
        return requestSampleIds;
    }

    public void setRequestSampleIds(List<Object> requestSampleIds) {
        this.requestSampleIds = requestSampleIds;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public List<ReportSample> getRnaReportSamples() {
        return rnaReportSamples;
    }

    public void setRnaReportSamples(List<ReportSample> rnaReportSamples) {
        this.rnaReportSamples = rnaReportSamples;
    }

    public List<ReportSample> getDnaReportSamples() {
        return dnaReportSamples;
    }

    public void setDnaReportSamples(List<ReportSample> dnaReportSamples) {
        this.dnaReportSamples = dnaReportSamples;
    }

    public List<ReportSample> getLibraryReportSamples() {
        return libraryReportSamples;
    }

    public void setLibraryReportSamples(List<ReportSample> libraryReportSamples) {
        this.libraryReportSamples = libraryReportSamples;
    }

    public List<PathologySample> getPathologyReportSamples() {
        return pathologyReportSamples;
    }

    public void setPathologyReportSamples(List<PathologySample> pathologyReportSamples) {
        this.pathologyReportSamples = pathologyReportSamples;
    }

    public List<HashMap<String, Object>> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<HashMap<String, Object>> attachments) {
        this.attachments = attachments;
    }

    public static class ReportSample {

        public String sampleId;
        public Long recordId;
        public String otherSampleId;
        public String altId;
        public String userSampleID;
        public String concentrationUnits;
        public String preservation;
        public String recipe;
        public String igoQcRecommendation;
        public String investigatorDecision;
        public String comments;
        public Long dateCreated;
        public Double concentration;
        public Double volume;
        public Double totalMass;
        public Boolean hideFromSampleQC;

        public ReportSample() {
        }

        public ReportSample(Map<String, Object> sampleFields) {
            this.sampleId = (String) sampleFields.get("SampleId");
            this.recordId = (Long) sampleFields.get("RecordId");
            this.otherSampleId = (String) sampleFields.get("OtherSampleId");
            this.altId = (String) sampleFields.get("AltId");
            this.userSampleID = (String) sampleFields.get("UserSampleID");
            this.concentration = (Double) sampleFields.get("Concentration");
            this.concentrationUnits = (String) sampleFields.get("ConcentrationUnits");
            this.volume = (Double) sampleFields.get("Volume");
            this.totalMass = (Double) sampleFields.get("TotalMass");
            this.preservation = (String) sampleFields.get("Preservation");
            this.recipe = (String) sampleFields.get("Recipe");
            this.igoQcRecommendation = (String) sampleFields.get("IgoQcRecommendation");
            this.investigatorDecision = (String) sampleFields.get("InvestigatorDecision");
            this.comments = (String) sampleFields.get("Comments");
            this.dateCreated = (Long) sampleFields.get("DateCreated");
            this.hideFromSampleQC = (Boolean) sampleFields.get("HideFromSampleQC");
        }

        public static class RnaReportSample extends ReportSample {
            public String rin;
            public Double rqn;
            public Double dV200;

            public RnaReportSample(Map<String, Object> sampleFields) {
                super(sampleFields);
                this.rin = (String) sampleFields.get("RIN");
                this.rqn = (Double) sampleFields.get("RQN");
                this.dV200 = (Double) sampleFields.get("DV200");
            }
        }

        public static class DnaReportSample extends ReportSample {
            public String tumorOrNormal;
            public String specimenType;
            public Double din;

            public DnaReportSample(Map<String, Object> sampleFields) {
                super(sampleFields);
                this.tumorOrNormal = (String) sampleFields.get("TumorOrNormal");
                this.specimenType = (String) sampleFields.get("SpecimenType");
                this.din = (Double) sampleFields.get("DIN");
            }

        }

        public static class LibraryReportSample extends ReportSample {
            public String tumorOrNormal;
            public Double avgSize;

            public LibraryReportSample(Map<String, Object> sampleFields) {
                super(sampleFields);
                this.tumorOrNormal = (String) sampleFields.get("TumorOrNormal");
                this.avgSize = (Double) sampleFields.get("AvgSize");
            }

        }
    }

    public static class PathologySample {

        public String sampleId;
        public Long recordId;
        public String otherSampleId;
        public String sampleStatus;

        public PathologySample() {
        }

        public PathologySample(Map<String, Object> sampleFields) {

            this.sampleId = (String) sampleFields.get("SampleId");
            this.recordId = (Long) sampleFields.get("RecordId");
            this.otherSampleId = (String) sampleFields.get("OtherSampleId");
            this.sampleStatus = (String) sampleFields.get("SampleFinalQCStatus");
        }


    }

}
