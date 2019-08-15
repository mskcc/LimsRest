package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Columns from multiple Picard files displayed together on the QC site.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QCSiteStats {
    // Picard File
    public String run;
    public String request;
    public String sample;
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
        qc.setStartingAmount(100);
        qc.setRecordId(2);
        qc.setRun(run);
        qc.setSampleName(sample);

        qc.setPercentAdapters(PCT_ADAPTER);
        qc.setUnpairedReadsExamined(unpairedReads);

        qc.setReadsExamined(READ_PAIRS_EXAMINED);
        qc.setUnmapped(UNMAPPED_READS);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QCSiteStats that = (QCSiteStats) o;

        if (!run.equals(that.run)) return false;
        if (!request.equals(that.request)) return false;
        return sample.equals(that.sample);
    }

    @Override
    public int hashCode() {
        int result = run.hashCode();
        result = 31 * result + request.hashCode();
        result = 31 * result + sample.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "QCSiteStats{" +
                "run='" + run + '\'' +
                ", request='" + request + '\'' +
                ", sample='" + sample + '\'' +
                ", referenceGenome='" + referenceGenome + '\'' +
                ", PCT_ADAPTER=" + PCT_ADAPTER +
                ", unpairedReads=" + unpairedReads +
                ", READ_PAIRS_EXAMINED=" + READ_PAIRS_EXAMINED +
                ", UNMAPPED_READS=" + UNMAPPED_READS +
                ", PERCENT_DUPLICATION=" + PERCENT_DUPLICATION +
                ", MEAN_COVERAGE=" + MEAN_COVERAGE +
                ", PCT_EXC_MAPQ=" + PCT_EXC_MAPQ +
                ", PCT_EXC_DUPE=" + PCT_EXC_DUPE +
                ", PCT_EXC_BASEQ=" + PCT_EXC_BASEQ +
                ", PCT_EXC_TOTAL=" + PCT_EXC_TOTAL +
                ", PCT_10X=" + PCT_10X +
                ", PCT_30X=" + PCT_30X +
                ", PCT_40X=" + PCT_40X +
                ", PCT_80X=" + PCT_80X +
                ", PCT_100X=" + PCT_100X +
                ", PCT_UTR_BASES=" + PCT_UTR_BASES +
                ", PCT_INTRONIC_BASES=" + PCT_INTRONIC_BASES +
                ", PCT_INTERGENIC_BASES=" + PCT_INTERGENIC_BASES +
                ", MEAN_TARGET_COVERAGE=" + MEAN_TARGET_COVERAGE +
                ", ZERO_CVG_TARGETS_PCT=" + ZERO_CVG_TARGETS_PCT +
                ", PCT_OFF_BAIT=" + PCT_OFF_BAIT +
                '}';
    }

    public String getReferenceGenome() {
        return referenceGenome;
    }

    public void setReferenceGenome(String referenceGenome) {
        this.referenceGenome = referenceGenome;
    }

    public long getUnpairedReads() {
        return unpairedReads;
    }

    public void setUnpairedReads(long unpairedReads) {
        this.unpairedReads = unpairedReads;
    }

    public String getRun() {
        return run;
    }

    public void setRun(String run) {
        this.run = run;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public double getPCT_ADAPTER() {
        return PCT_ADAPTER;
    }

    public void setPCT_ADAPTER(double PCT_ADAPTER) {
        this.PCT_ADAPTER = PCT_ADAPTER;
    }

    public long getREAD_PAIRS_EXAMINED() {
        return READ_PAIRS_EXAMINED;
    }

    public void setREAD_PAIRS_EXAMINED(long READ_PAIRS_EXAMINED) {
        this.READ_PAIRS_EXAMINED = READ_PAIRS_EXAMINED;
    }

    public long getUNMAPPED_READS() {
        return UNMAPPED_READS;
    }

    public void setUNMAPPED_READS(long UNMAPPED_READS) {
        this.UNMAPPED_READS = UNMAPPED_READS;
    }

    public Double getPERCENT_DUPLICATION() {
        return PERCENT_DUPLICATION;
    }

    public void setPERCENT_DUPLICATION(Double PERCENT_DUPLICATION) {
        this.PERCENT_DUPLICATION = PERCENT_DUPLICATION;
    }

    public Double getMEAN_COVERAGE() {
        return MEAN_COVERAGE;
    }

    public void setMEAN_COVERAGE(Double MEAN_COVERAGE) {
        this.MEAN_COVERAGE = MEAN_COVERAGE;
    }

    public Double getPCT_10X() {
        return PCT_10X;
    }

    public void setPCT_10X(Double PCT_10X) {
        this.PCT_10X = PCT_10X;
    }

    public Double getPCT_30X() {
        return PCT_30X;
    }

    public void setPCT_30X(Double PCT_30X) {
        this.PCT_30X = PCT_30X;
    }

    public Double getPCT_100X() {
        return PCT_100X;
    }

    public void setPCT_100X(Double PCT_100X) {
        this.PCT_100X = PCT_100X;
    }

    public Double getPCT_UTR_BASES() {
        return PCT_UTR_BASES;
    }

    public void setPCT_UTR_BASES(Double PCT_UTR_BASES) {
        this.PCT_UTR_BASES = PCT_UTR_BASES;
    }

    public Double getPCT_INTRONIC_BASES() {
        return PCT_INTRONIC_BASES;
    }

    public void setPCT_INTRONIC_BASES(Double PCT_INTRONIC_BASES) {
        this.PCT_INTRONIC_BASES = PCT_INTRONIC_BASES;
    }

    public Double getPCT_INTERGENIC_BASES() {
        return PCT_INTERGENIC_BASES;
    }

    public void setPCT_INTERGENIC_BASES(Double PCT_INTERGENIC_BASES) {
        this.PCT_INTERGENIC_BASES = PCT_INTERGENIC_BASES;
    }

    public double getMEAN_TARGET_COVERAGE() {
        return MEAN_TARGET_COVERAGE;
    }

    public void setMEAN_TARGET_COVERAGE(double MEAN_TARGET_COVERAGE) {
        this.MEAN_TARGET_COVERAGE = MEAN_TARGET_COVERAGE;
    }

    public double getZERO_CVG_TARGETS_PCT() {
        return ZERO_CVG_TARGETS_PCT;
    }

    public void setZERO_CVG_TARGETS_PCT(double ZERO_CVG_TARGETS_PCT) {
        this.ZERO_CVG_TARGETS_PCT = ZERO_CVG_TARGETS_PCT;
    }

    public double getPCT_OFF_BAIT() {
        return PCT_OFF_BAIT;
    }

    public void setPCT_OFF_BAIT(double PCT_OFF_BAIT) {
        this.PCT_OFF_BAIT = PCT_OFF_BAIT;
    }
}