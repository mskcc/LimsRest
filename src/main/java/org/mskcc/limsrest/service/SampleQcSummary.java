package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.*;
import lombok.Setter;

@Setter
public class SampleQcSummary {
    private String sampleName; // OtherSampleId
    private String run;
    private String qcStatus;
    private Long createDate;
    private String baitSet;
    private String qcUnits;
    private String quantUnits;
    private double MEAN_COVERAGE;
    private double median_COVERAGE;
    private double PCT_EXC_MAPQ;
    private double PCT_EXC_DUPE;
    private double PCT_EXC_BASEQ;
    private double PCT_EXC_TOTAL;
    private double meanTargetCoverage;
    private double percentAdapters;
    private double percentDuplication;
    private double percentOffBait;
    private double percentTarget10x;
    private double percentTarget30x;
    private double percentTarget40x;
    private double percentTarget80x;
    private double percentTarget100x;
    private double percentRibosomalBases;
    private double percentUtrBases;
    private double percentCodingBases;
    private double percentIntronicBases;
    private double percentIntergenicBases;
    private double percentMrnaBases;
    private Double quantIt;
    private Double qcControl;
    private Double startingAmount;
    private long readsDuped;
    private long readsExamined;
    private long unmappedReads;
    private long unpairedExamined;
    private long recordId;
    private double zeroCoveragePercent;
    private Long totalReads;
    private boolean reviewed;
    private String recipe;
    private String statsVersion;

    public SampleQcSummary() {
        sampleName = "ERROR";
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getMEAN_COVERAGE() { return MEAN_COVERAGE; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPCT_EXC_MAPQ() { return PCT_EXC_MAPQ; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPCT_EXC_DUPE() { return PCT_EXC_DUPE; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPCT_EXC_BASEQ() { return PCT_EXC_BASEQ; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPCT_EXC_TOTAL() { return PCT_EXC_TOTAL; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getBaitSet() {
        return this.baitSet;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getQcUnits() {
        return this.qcUnits;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getQuantUnits() {
        return this.quantUnits;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getCreateDate() {
        return this.createDate;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getMeanTargetCoverage() {
        return this.meanTargetCoverage;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getMedian_COVERAGE() { return this.median_COVERAGE; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentAdapters() {
        return this.percentAdapters;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentDuplication() {
        return this.percentDuplication;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentOffBait() {
        return this.percentOffBait;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentTarget10x() {
        return this.percentTarget10x;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentTarget30x() { return this.percentTarget30x; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentTarget40x() { return this.percentTarget40x; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentTarget80x() { return this.percentTarget80x; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentTarget100x() {
        return this.percentTarget100x;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentRibosomalBases() {
        return this.percentRibosomalBases;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentCodingBases() {
        return this.percentCodingBases;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentIntronicBases() {
        return this.percentIntronicBases;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentIntergenicBases() {
        return this.percentIntergenicBases;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentUtrBases() {
        return this.percentUtrBases;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentMrnaBases() {
        return this.percentMrnaBases;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Double getStartingAmount() {
        return this.startingAmount;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Double getQcControl() {
        return this.qcControl;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Double getQuantIt() {
        return this.quantIt;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getReadsDuped() {
        return this.readsDuped;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getReadsExamined() {
        return this.readsExamined;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getTotalReads() {
        return this.totalReads;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getUnmapped() {
        return this.unmappedReads;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getUnpairedReadsExamined() {
        return this.unpairedExamined;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getZeroCoveragePercent() {
        return this.zeroCoveragePercent;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRun() {
        return this.run;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleName() {
        return this.sampleName;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public boolean getReviewed() {
        return this.reviewed;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getRecordId() {
        return this.recordId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getQcStatus() {
        return this.qcStatus;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRecipe() {
        return this.recipe;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getStatsVersion() {
        return this.statsVersion;
    }
}