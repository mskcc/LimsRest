package org.mskcc.limsrest.service.sequencingqc;

import java.util.HashMap;
import java.util.Map;

public class SampleSequencingQc {

    String sampleId;
    String otherSampleId;
    String request;
    String baitSet;
    String sequencerRunFolder;
    String seqQCStatus;
    long readsExamined;
    long totalReads;
    long unmappedDupes;
    long readPairDupes;
    long unpairedReads;
    double meanCoverage;
    double meanTargetCoverage;
    double percentTarget100X;
    double percentTarget30X;
    double percentTarget10X;
    double percentAdapters;
    double percentCodingBases;
    double percentExcBaseQ;
    double percentExcDupe;
    double percentExcMapQ;
    double percentExcTotal;
    double percentIntergenicBases;
    double percentIntronicBases;
    double percentMrnaBases;
    double percentOffBait;
    double percentRibosomalBases;
    double percentUtrBases;
    double percentDuplication;
    double zeroCoveragePercent;
    double mskq;
    long genomeTerritory;
    double gRefOxoQ;

    public SampleSequencingQc(String sampleId, String otherSampleId, String request,
                              String baitSet, String sequencerRunFolder, String seqQCStatus, long readsExamined,
                              long totalReads, long unmappedDupes, long readPairDupes, long unpairedReads, double meanCoverage,
                              double meanTargetCoverage, double percentTarget100X, double percentTarget30X, double percentTarget10X,
                              double percentAdapters, double percentCodingBases, double percentExcBaseQ, double percentExcDupe,
                              double percentExcMapQ, double percentExcTotal, double percentIntergenicBases, double percentIntronicBases,
                              double percentMrnaBases, double percentOffBait, double percentRibosomalBases, double percentUtrBases,
                              double percentDuplication, double zeroCoveragePercent, double mskq, long genomeTerritory, double gRefOxoQ) {
        this.sampleId = sampleId;
        this.otherSampleId = otherSampleId;
        this.request = request;
        this.baitSet = baitSet;
        this.sequencerRunFolder = sequencerRunFolder;
        this.seqQCStatus = seqQCStatus;
        this.readsExamined = readsExamined;
        this.totalReads = totalReads;
        this.unmappedDupes = unmappedDupes;
        this.readPairDupes = readPairDupes;
        this.unpairedReads = unpairedReads;
        this.meanCoverage = meanCoverage;
        this.meanTargetCoverage = meanTargetCoverage;
        this.percentTarget100X = percentTarget100X;
        this.percentTarget30X = percentTarget30X;
        this.percentTarget10X = percentTarget10X;
        this.percentAdapters = percentAdapters;
        this.percentCodingBases = percentCodingBases;
        this.percentExcBaseQ = percentExcBaseQ;
        this.percentExcDupe = percentExcDupe;
        this.percentExcMapQ = percentExcMapQ;
        this.percentExcTotal = percentExcTotal;
        this.percentIntergenicBases = percentIntergenicBases;
        this.percentIntronicBases = percentIntronicBases;
        this.percentMrnaBases = percentMrnaBases;
        this.percentOffBait = percentOffBait;
        this.percentRibosomalBases = percentRibosomalBases;
        this.percentUtrBases = percentUtrBases;
        this.percentDuplication = percentDuplication;
        this.zeroCoveragePercent = zeroCoveragePercent;
        this.mskq = mskq;
        this.genomeTerritory = genomeTerritory;
        this.gRefOxoQ = gRefOxoQ;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getOtherSampleId() {
        return otherSampleId;
    }

    public void setOtherSampleId(String otherSampleId) {
        this.otherSampleId = otherSampleId;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getBaitSet() {
        return baitSet;
    }

    public void setBaitSet(String baitSet) {
        this.baitSet = baitSet;
    }

    public String getSequencerRunFolder() {
        return sequencerRunFolder;
    }

    public void setSequencerRunFolder(String sequencerRunFolder) {
        this.sequencerRunFolder = sequencerRunFolder;
    }

    public String getSeqQCStatus() {
        return seqQCStatus;
    }

    public void setSeqQCStatus(String seqQCStatus) {
        this.seqQCStatus = seqQCStatus;
    }

    public long getReadsExamined() {
        return readsExamined;
    }

    public void setReadsExamined(long readsExamined) {
        this.readsExamined = readsExamined;
    }

    public long getTotalReads() {
        return totalReads;
    }

    public void setTotalReads(long totalReads) {
        this.totalReads = totalReads;
    }

    public long getUnmappedDupes() {
        return unmappedDupes;
    }

    public void setUnmappedDupes(long unmappedDupes) {
        this.unmappedDupes = unmappedDupes;
    }

    public long getReadPairDupes() {
        return readPairDupes;
    }

    public void setReadPairDupes(long readPairDupes) {
        this.readPairDupes = readPairDupes;
    }

    public long getUnpairedReads() {
        return unpairedReads;
    }

    public void setUnpairedReads(long unpairedReads) {
        this.unpairedReads = unpairedReads;
    }

    public double getMeanCoverage() {
        return meanCoverage;
    }

    public void setMeanCoverage(double meanCoverage) {
        this.meanCoverage = meanCoverage;
    }

    public double getMeanTargetCoverage() {
        return meanTargetCoverage;
    }

    public void setMeanTargetCoverage(double meanTargetCoverage) {
        this.meanTargetCoverage = meanTargetCoverage;
    }

    public double getPercentTarget100X() {
        return percentTarget100X;
    }

    public void setPercentTarget100X(double percentTarget100X) {
        this.percentTarget100X = percentTarget100X;
    }

    public double getPercentTarget10X() {
        return percentTarget10X;
    }

    public void setPercentTarget10X(double percentTarget10X) {
        this.percentTarget10X = percentTarget10X;
    }

    public double getPercentTarget30X() {
        return percentTarget30X;
    }

    public void setPercentTarget30X(double percentTarget30X) {
        this.percentTarget30X = percentTarget30X;
    }

    public double getPercentAdapters() {
        return percentAdapters;
    }

    public void setPercentAdapters(double percentAdapters) {
        this.percentAdapters = percentAdapters;
    }

    public double getPercentCodingBases() {
        return percentCodingBases;
    }

    public void setPercentCodingBases(double percentCodingBases) {
        this.percentCodingBases = percentCodingBases;
    }

    public double getPercentExcBaseQ() {
        return percentExcBaseQ;
    }

    public void setPercentExcBaseQ(double percentExcBaseQ) {
        this.percentExcBaseQ = percentExcBaseQ;
    }

    public double getPercentExcDupe() {
        return percentExcDupe;
    }

    public void setPercentExcDupe(double percentExcDupe) {
        this.percentExcDupe = percentExcDupe;
    }

    public double getPercentExcMapQ() {
        return percentExcMapQ;
    }

    public void setPercentExcMapQ(double percentExcMapQ) {
        this.percentExcMapQ = percentExcMapQ;
    }

    public double getPercentExcTotal() {
        return percentExcTotal;
    }

    public void setPercentExcTotal(double percentExcTotal) {
        this.percentExcTotal = percentExcTotal;
    }

    public double getPercentIntergenicBases() {
        return percentIntergenicBases;
    }

    public void setPercentIntergenicBases(double percentIntergenicBases) {
        this.percentIntergenicBases = percentIntergenicBases;
    }

    public double getPercentIntronicBases() {
        return percentIntronicBases;
    }

    public void setPercentIntronicBases(double percentIntronicBases) {
        this.percentIntronicBases = percentIntronicBases;
    }

    public double getPercentMrnaBases() {
        return percentMrnaBases;
    }

    public void setPercentMrnaBases(double percentMrnaBases) {
        this.percentMrnaBases = percentMrnaBases;
    }

    public double getPercentOffBait() {
        return percentOffBait;
    }

    public void setPercentOffBait(double percentOffBait) {
        this.percentOffBait = percentOffBait;
    }

    public double getPercentRibosomalBases() {
        return percentRibosomalBases;
    }

    public void setPercentRibosomalBases(double percentRibosomalBases) {
        this.percentRibosomalBases = percentRibosomalBases;
    }

    public double getPercentUtrBases() {
        return percentUtrBases;
    }

    public void setPercentUtrBases(double percentUtrBases) {
        this.percentUtrBases = percentUtrBases;
    }

    public double getPercentDuplication() {
        return percentDuplication;
    }

    public void setPercentDuplication(double percentDuplication) {
        this.percentDuplication = percentDuplication;
    }

    public double getZeroCoveragePercent() {
        return zeroCoveragePercent;
    }

    public void setZeroCoveragePercent(double zeroCoveragePercent) {
        this.zeroCoveragePercent = zeroCoveragePercent;
    }

    public double getMskq() {
        return mskq;
    }

    public void setMskq(double mskq) {
        this.mskq = mskq;
    }

    public long getGenomeTerritory() {
        return genomeTerritory;
    }

    public void setGenomeTerritory(long genomeTerritory) {
        this.genomeTerritory = genomeTerritory;
    }

    public double getgRefOxoQ() {
        return gRefOxoQ;
    }

    public void setgRefOxoQ(double gRefOxoQ) {
        this.gRefOxoQ = gRefOxoQ;
    }

    public Map<String, Object> getSequencingQcValues(){
        Map<String, Object> qcValues = new HashMap<>();
        qcValues.put("SampleId", this.sampleId);
        qcValues.put("OtherSampleId", this.otherSampleId);
        qcValues.put("Request", this.request);
        qcValues.put("BaitSet", this.baitSet);
        qcValues.put("SequencerRunFolder", this.sequencerRunFolder);
        qcValues.put("SeqQCStatus", this.seqQCStatus);
        qcValues.put("ReadsExamined", this.readsExamined);
        qcValues.put("TotalReads", this.totalReads);
        qcValues.put("UnmappedDupes", this.unmappedDupes);
        qcValues.put("ReadPairDupes", this.readPairDupes);
        qcValues.put("UnpairedReads", this.unpairedReads);
        qcValues.put("MeanCoverage", this.meanCoverage);
        qcValues.put("MeanTargetCoverage", this.meanTargetCoverage);
        qcValues.put("PercentTarget100X", this.percentTarget100X);
        qcValues.put("PercentTarget30X", this.percentTarget30X);
        qcValues.put("PercentTarget10X", this.percentTarget10X);
        qcValues.put("PercentAdapters", this.percentAdapters);
        qcValues.put("PercentCodingBases", this.percentCodingBases);
        qcValues.put("PercentExcBaseQ", this.percentExcBaseQ);
        qcValues.put("PercentExcDupe",  this.percentExcDupe);
        qcValues.put("PercentExcMapQ", this.percentExcMapQ);
        qcValues.put("PercentExcTotal", this.percentExcTotal);
        qcValues.put("PercentIntergenicBases", this.percentIntergenicBases);
        qcValues.put("PercentIntronicBases", this.percentIntronicBases);
        qcValues.put("PercentMrnaBases", this.percentMrnaBases);
        qcValues.put("PercentOffBait", this.percentOffBait);
        qcValues.put("PercentRibosomalBases", this.percentRibosomalBases);
        qcValues.put("PercentUtrBases", this.percentUtrBases);
        qcValues.put("PercentDuplication", this.percentDuplication);
        qcValues.put("ZeroCoveragePercent", this.zeroCoveragePercent);
        qcValues.put("Mskq", this.mskq);
        qcValues.put("GenomeTerritory", this.genomeTerritory);
        qcValues.put("GRefOxoQ", this.gRefOxoQ);
        return qcValues;
    }
}
