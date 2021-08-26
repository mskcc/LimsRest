package org.mskcc.limsrest.service.sequencingqc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
@Getter @Setter @AllArgsConstructor
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
