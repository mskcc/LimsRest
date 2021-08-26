package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Columns from multiple Picard files displayed together on the QC site.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter @Setter
@ToString @EqualsAndHashCode
public class QCSiteStats {
    // Picard File
    @EqualsAndHashCode.Include public String run;
    @EqualsAndHashCode.Include public String request;
    @EqualsAndHashCode.Include public String sample;
    public String referenceGenome;

    // ALIGNMENT SUMMARY METRICS
    public double PCT_ADAPTER;
    public long unpairedReads; // derived field from summary metrics

    // Duplication Metrics
    public long READ_PAIRS_EXAMINED;
    public long UNMAPPED_READS;
    public Double PERCENT_DUPLICATION;

    // WGS
    public Double MEAN_COVERAGE;
    public Double PCT_EXC_MAPQ;
    public Double PCT_EXC_DUPE;
    public Double PCT_EXC_BASEQ;
    public Double PCT_EXC_TOTAL;
    public Double PCT_10X;
    public Double PCT_30X;
    public Double PCT_40X;
    public Double PCT_80X;
    public Double PCT_100X;

    // RNASEQ
    public Double PCT_UTR_BASES;
    public Double PCT_INTRONIC_BASES;
    public Double PCT_INTERGENIC_BASES;

    // HS METRICS
    public double MEAN_TARGET_COVERAGE;
    public double ZERO_CVG_TARGETS_PCT;
    public double PCT_OFF_BAIT;


    public QCSiteStats() {
    }

    // Convert to type returned to QCSite
    public SampleQcSummary toSampleSummary() {
        SampleQcSummary qc = new SampleQcSummary();
        qc.setStartingAmount(100.0);
        qc.setRecordId(2);
        qc.setRun(run);
        qc.setSampleName(sample);

        qc.setPercentAdapters(PCT_ADAPTER);
        qc.setUnpairedExamined(unpairedReads);

        qc.setReadsExamined(READ_PAIRS_EXAMINED);
        qc.setUnmappedReads(UNMAPPED_READS);
        if (PERCENT_DUPLICATION != null)
            qc.setPercentDuplication(PERCENT_DUPLICATION);

        if (MEAN_COVERAGE != null)
            qc.setMEAN_COVERAGE(MEAN_COVERAGE);
        if (PCT_EXC_MAPQ != null)
            qc.setPCT_EXC_MAPQ(PCT_EXC_MAPQ);
        if (PCT_EXC_DUPE != null)
            qc.setPCT_EXC_DUPE(PCT_EXC_DUPE);
        if (PCT_EXC_BASEQ != null)
            qc.setPCT_EXC_BASEQ(PCT_EXC_BASEQ);
        if (PCT_EXC_TOTAL != null)
            qc.setPCT_EXC_TOTAL(PCT_EXC_TOTAL);
        if (PCT_10X != null)
            qc.setPercentTarget10x(PCT_10X);
        if (PCT_30X != null)
            qc.setPercentTarget30x(PCT_30X);
        if (PCT_40X != null)
            qc.setPercentTarget40x(PCT_40X);
        if (PCT_80X != null)
            qc.setPercentTarget80x(PCT_80X);
        if (PCT_100X != null)
            qc.setPercentTarget100x(PCT_100X);

        if (PCT_UTR_BASES != null)
            qc.setPercentUtrBases(PCT_UTR_BASES);
        if (PCT_INTRONIC_BASES != null)
            qc.setPercentIntronicBases(PCT_INTRONIC_BASES);
        if (PCT_INTERGENIC_BASES != null)
            qc.setPercentIntergenicBases(PCT_INTERGENIC_BASES);

        qc.setMeanTargetCoverage(MEAN_TARGET_COVERAGE);
        qc.setZeroCoveragePercent(ZERO_CVG_TARGETS_PCT);
        qc.setPercentOffBait(PCT_OFF_BAIT);

        qc.setQcStatus("Passed");
        qc.setReviewed(true);
        qc.setTotalReads(1L);
        qc.setQcUnits("ng/uL");
        qc.setQcControl(1.0);
        return qc;
    }
}