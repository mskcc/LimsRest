package org.mskcc.limsrest.service;


import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// base class for DNA/RNA/Library Report Samples
@Getter @Setter
public class QcReportSampleList {
    public String requestId;
    public List<String> requestSampleIds;
    public List<ReportSample> dnaReportSamples;
    public List<ReportSample> rnaReportSamples;
    public List<ReportSample> libraryReportSamples;
    public List<ReportSample> poolReportSamples;
    public List<PathologySample> pathologyReportSamples;
    public List<CovidSample> covidReportSamples;
    public List<HashMap<String, Object>> attachments;

    public QcReportSampleList() {
    }

    public QcReportSampleList(String requestId, List<String> requestSampleIds) {
        this.requestId = requestId;
        this.requestSampleIds = requestSampleIds;
        dnaReportSamples = new ArrayList<>();
        rnaReportSamples = new ArrayList<>();
        libraryReportSamples = new ArrayList<>();
        poolReportSamples = new ArrayList<>();
        pathologyReportSamples = new ArrayList<>();
        attachments = new ArrayList<>();
    }

    public QcReportSampleList(String requestId) {
        this.requestId = requestId;
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
            public String sourceSampleId;
            public Double A260280;
            public Double A260230;

            public RnaReportSample(Map<String, Object> sampleFields) {
                super(sampleFields);
                this.rin = (String) sampleFields.get("RIN");
                this.rqn = (Double) sampleFields.get("RQN");
                this.dV200 = (Double) sampleFields.get("DV200");
                this.sourceSampleId = (String) sampleFields.get("SourceSampleId");
                this.A260230 = (Double) sampleFields.get("A260230");
                this.A260280 = (Double) sampleFields.get("A260280");
            }
        }

        public static class DnaReportSample extends ReportSample {
            public String tumorOrNormal;
            public Double humanPercentage;
            public String specimenType;
            public Double din;
            public String sourceSampleId;
            public Double A260280;
            public Double A260230;

            public DnaReportSample(Map<String, Object> sampleFields) {
                super(sampleFields);
                this.tumorOrNormal = (String) sampleFields.get("TumorOrNormal");
                this.humanPercentage = (Double) sampleFields.get("HumanPercentage");
                this.specimenType = (String) sampleFields.get("SpecimenType");
                this.din = (Double) sampleFields.get("DIN");
                this.sourceSampleId = (String) sampleFields.get("SourceSampleId");
                this.A260230 = (Double) sampleFields.get("A260230");
                this.A260280 = (Double) sampleFields.get("A260280");
            }

        }

        public static class LibraryReportSample extends ReportSample {
            public String tumorOrNormal;
            public Double avgSize;
            public String sourceSampleId;
            public String numOfReads;

            public LibraryReportSample(Map<String, Object> sampleFields) {
                super(sampleFields);
                this.tumorOrNormal = (String) sampleFields.get("TumorOrNormal");
                this.avgSize = (Double) sampleFields.get("AvgSize");
                this.sourceSampleId = (String) sampleFields.get("SourceSampleId");
                this.numOfReads = (String) sampleFields.get("NumOfReads");
            }

        }

        public static class PoolReportSample extends ReportSample {
            public String tumorOrNormal;
            public Double avgSize;
            public String numOfReads;

            public PoolReportSample(Map<String, Object> sampleFields) {
                super(sampleFields);
                this.tumorOrNormal = (String) sampleFields.get("TumorOrNormal");
                this.avgSize = (Double) sampleFields.get("AvgSize");
                this.numOfReads = (String) sampleFields.get("NumOfReads");
            }

        }
    }

    public static class PathologySample {

        public String sampleId;
        public Long recordId;
        public String otherSampleId;
        public String sampleStatus;
        public Boolean hideFromSampleQC;

        public PathologySample() {
        }

        public PathologySample(Map<String, Object> sampleFields) {

            this.sampleId = (String) sampleFields.get("SampleId");
            this.recordId = (Long) sampleFields.get("RecordId");
            this.otherSampleId = (String) sampleFields.get("OtherSampleId");
            this.sampleStatus = (String) sampleFields.get("SampleFinalQCStatus");
            this.hideFromSampleQC = (Boolean) sampleFields.get("HideFromSampleQC");

        }


    }

    public static class CovidSample {


        public Long recordId;
        public String otherSampleId;
        public String userSampleId;
        public String assayResult;
        public String cqN1;
        public String cqN2;
        public String cqRP;
        public Boolean hideFromSampleQC;

        public CovidSample() {
        }

        public CovidSample(Map<String, Object> sampleFields, String serviceId) {

            this.recordId = (Long) sampleFields.get("RecordId");
            this.otherSampleId = (String) sampleFields.get("OtherSampleId") ;
            this.userSampleId = this.otherSampleId.split("-IGO-")[0];
            this.assayResult = (String) sampleFields.get("AssayResult");
            this.cqN1 = (String) sampleFields.get("CqN1");
            this.cqN2 = (String) sampleFields.get("CqN2");
            this.cqRP = (String) sampleFields.get("CqRP");
            this.hideFromSampleQC = (Boolean) sampleFields.get("HideFromSampleQC");
        }

    }

}
