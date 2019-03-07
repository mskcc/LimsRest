package org.mskcc.limsrest.limsapi;

import com.fasterxml.jackson.annotation.*;

public class SampleQcSummary {
    private String baitSet;
    private String qcUnits;
    private String quantUnits;
    private double mskq;
    private double meanTargetCoverage;
    private double percentAdapters;
    private double percentDuplication;
    private double percentOffBait;
    private double percentTarget10x;
    private double percentTarget30x;
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
    private String run;
    private String sampleName;
    private boolean reviewed;
    private String qcStatus;
    private Long createDate;

    public SampleQcSummary() {
        sampleName = "ERROR";
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getBaitSet() {
        return this.baitSet;
    }

    public void setBaitSet(String baitSet) {
        this.baitSet = baitSet;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getQcUnits() {
        return this.qcUnits;
    }

    public void setQcUnits(String units) {
        this.qcUnits = units;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getQuantUnits() {
        return this.quantUnits;
    }

    public void setQuantUnits(String units) {
        this.quantUnits = units;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getMskq() {
        return this.mskq;
    }

    public void setMskq(double mskq) {
        this.mskq = mskq;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getMeanTargetCoverage() {
        return this.meanTargetCoverage;
    }

    public void setMeanTargetCoverage(double mtc) {
        this.meanTargetCoverage = mtc;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentAdapters() {
        return this.percentAdapters;
    }

    public void setPercentAdapters(double perAd) {
        this.percentAdapters = perAd;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentDuplication() {
        return this.percentDuplication;
    }

    public void setPercentDuplication(double perDup) {
        this.percentDuplication = perDup;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentOffBait() {
        return this.percentOffBait;
    }

    public void setPercentOffBait(double perOff) {
        this.percentOffBait = perOff;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentTarget10x() {
        return this.percentTarget10x;
    }

    public void setPercentTarget10x(double per10x) {
        this.percentTarget10x = per10x;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentTarget30x() {
        return this.percentTarget30x;
    }

    public void setPercentTarget30x(double per30x) {
        this.percentTarget30x = per30x;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentTarget100x() {
        return this.percentTarget100x;
    }

    public void setPercentTarget100x(double per100x) {
        this.percentTarget100x = per100x;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentRibosomalBases() {
        return this.percentRibosomalBases;
    }

    public void setPercentRibosomalBases(double perRibo) {
        this.percentRibosomalBases = perRibo;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentCodingBases() {
        return this.percentCodingBases;
    }

    public void setPercentCodingBases(double perC) {
        this.percentCodingBases = perC;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentIntronicBases() {
        return this.percentIntronicBases;
    }

    public void setPercentIntronicBases(double perI) {
        this.percentIntronicBases = perI;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentIntergenicBases() {
        return this.percentIntergenicBases;
    }

    public void setPercentIntergenicBases(double perI) {
        this.percentIntergenicBases = perI;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentUtrBases() {
        return this.percentUtrBases;
    }

    public void setPercentUtrBases(double perU) {
        this.percentUtrBases = perU;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getPercentMrnaBases() {
        return this.percentMrnaBases;
    }

    public void setPercentMrnaBases(double perM) {
        this.percentMrnaBases = perM;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Double getStartingAmount() {
        return this.startingAmount;
    }

    public void setStartingAmount(double start) {
        this.startingAmount = start;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Double getQcControl() {
        return this.qcControl;
    }

    public void setQcControl(Double qcc) {
        this.qcControl = qcc;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Double getQuantIt() {
        return this.quantIt;
    }

    public void setQuantIt(Double quant) {
        this.quantIt = quant;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getReadsDuped() {
        return this.readsDuped;
    }

    public void setReadsDuped(long dup) {
        this.readsDuped = dup;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getReadsExamined() {
        return this.readsExamined;
    }

    public void setReadsExamined(long ex) {
        this.readsExamined = ex;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getTotalReads() {
        return this.totalReads;
    }

    public void setTotalReads(Long total) {
        this.totalReads = total;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getUnmapped() {
        return this.unmappedReads;
    }

    public void setUnmapped(long unmapped) {
        this.unmappedReads = unmapped;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getUnpairedReadsExamined() {
        return this.unpairedExamined;
    }

    public void setUnpairedReadsExamined(long unpaired) {
        this.unpairedExamined = unpaired;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getZeroCoveragePercent() {
        return this.zeroCoveragePercent;
    }

    public void setZeroCoveragePercent(double perZero) {
        this.zeroCoveragePercent = perZero;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRun() {
        return this.run;
    }

    public void setRun(String run) {
        this.run = run;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleName() {
        return this.sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public boolean getReviewed() {
        return this.reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getRecordId() {
        return this.recordId;
    }

    public void setRecordId(long recordId) {
        this.recordId = recordId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getQcStatus() {
        return this.qcStatus;
    }

    public void setQcStatus(String qcStatus) {
        this.qcStatus = qcStatus;
    }
}