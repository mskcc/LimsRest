package org.mskcc.limsrest.service.analytics;

public class SequencingSampleData {
    String sampleId;
    String otherSampleId;
    String sampleType;
    String specimenType;
    String recipe;
    String tumornormal;
    Integer requestedReads;
    Integer requestedCoverage;
    String runType;
    String readLength;
    String sequencingMachine;
    Double concentrationNucleicAcid;
    Double concentrationLibrary;
    Integer readsExamined;
    Integer unpairedReadsExamined;
    Integer totalReads;
    Double pctMrnaBases;
    Double pctIntragenicBases;
    Double pctUtrBases;
    Double pctCodingBases;
    Double pctRiboBases;
    Integer unmappedReads;
    Integer readPairDuplicates;
    Double mskq;
    Double pctTarget100x;
    Double pctTarget80x;
    Double pctTarget40x;
    Double pctTarget30x;
    Double pctTarget10x;
    Double meanTargetCoverage;
    Double pctOffBait;
    Double pctAdapters;


//    public SequencingSampleData(
//            String sampleId, String otherSampleId, String sampleType, String specimenType, String recipe, String tumornormal, Integer requestedReads, Integer requestedCoverage,
//            String runType, String readLength, String sequencingMachine, Double concentrationNucleicAcid, Double concentrationLibrary, Integer readsExamined, Integer unpairedReadsExamined,
//            Integer totalReads, Double pctMrnaBases, Double pctIntragenicBases, Double pctUtrBases, Double pctCodingBases, Double pctRiboBases, Integer unmappedReads, Integer readPairDuplicates,
//            Double mskq, Double pctTarget100x, Double pctTarget80x, Double pctTarget40x, Double pctTarget30x, Double pctTarget10x, Double meanTargetCoverage,Double pctOffBait, Double pctAdapters
//    ) {
//        this.sampleId = sampleId;
//        this.otherSampleId = otherSampleId;
//        this.sampleType = sampleType;
//        this.specimenType = specimenType;
//        this.recipe = recipe;
//        this.tumornormal = tumornormal;
//        this.requestedReads = requestedReads;
//        this.requestedCoverage = requestedCoverage;
//        this.runType = runType;
//        this.readLength = readLength;
//        this.sequencingMachine = sequencingMachine;
//        this.concentrationNucleicAcid = concentrationNucleicAcid;
//        this.concentrationLibrary = concentrationLibrary;
//        this.readsExamined = readsExamined;
//        this.unpairedReadsExamined = unpairedReadsExamined;
//        this.totalReads = totalReads;
//        this.pctMrnaBases = pctMrnaBases;
//        this.pctIntragenicBases = pctIntragenicBases;
//        this.pctUtrBases = pctUtrBases;
//        this.pctCodingBases = pctCodingBases;
//        this.pctRiboBases = pctRiboBases;
//        this.unmappedReads = unmappedReads;
//        this.mskq = mskq;
//        this.pctTarget100x = pctTarget100x;
//        this.pctTarget80x = pctTarget80x;
//        this.pctTarget40x = pctTarget40x;
//        this.pctTarget30x = pctTarget30x;
//        this.pctTarget10x = pctTarget10x;
//        this.meanTargetCoverage = meanTargetCoverage;
//        this.pctOffBait = pctOffBait;
//        this.pctAdapters = pctAdapters;
//    }

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

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getSpecimenType() {
        return specimenType;
    }

    public void setSpecimenType(String specimenType) {
        this.specimenType = specimenType;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public String getTumornormal() {
        return tumornormal;
    }

    public void setTumornormal(String tumornormal) {
        this.tumornormal = tumornormal;
    }

    public Integer getRequestedReads() {
        return requestedReads;
    }

    public void setRequestedReads(Integer requestedReads) {
        this.requestedReads = requestedReads;
    }

    public Integer getRequestedCoverage() {
        return requestedCoverage;
    }

    public void setRequestedCoverage(Integer requestedCoverage) {
        this.requestedCoverage = requestedCoverage;
    }

    public String getRunType() {
        return runType;
    }

    public void setRunType(String runType) {
        this.runType = runType;
    }

    public String getReadLength() {
        return readLength;
    }

    public void setReadLength(String readLength) {
        this.readLength = readLength;
    }

    public String getSequencingMachine() {
        return sequencingMachine;
    }

    public void setSequencingMachine(String sequencingMachine) {
        this.sequencingMachine = sequencingMachine;
    }

    public Double getConcentrationNucleicAcid() {
        return concentrationNucleicAcid;
    }

    public void setConcentrationNucleicAcid(Double concentrationNucleicAcid) {
        this.concentrationNucleicAcid = concentrationNucleicAcid;
    }

    public Double getConcentrationLibrary() {
        return concentrationLibrary;
    }

    public void setConcentrationLibrary(Double concentrationLibrary) {
        this.concentrationLibrary = concentrationLibrary;
    }

    public Integer getReadsExamined() {
        return readsExamined;
    }

    public void setReadsExamined(Integer readsExamined) {
        this.readsExamined = readsExamined;
    }

    public Integer getUnpairedReadsExamined() {
        return unpairedReadsExamined;
    }

    public void setUnpairedReadsExamined(Integer unpairedReadsExamined) {
        this.unpairedReadsExamined = unpairedReadsExamined;
    }

    public Integer getTotalReads() {
        return totalReads;
    }

    public void setTotalReads(Integer totalReads) {
        this.totalReads = totalReads;
    }

    public Double getPctMrnaBases() {
        return pctMrnaBases;
    }

    public void setPctMrnaBases(Double pctMrnaBases) {
        this.pctMrnaBases = pctMrnaBases;
    }

    public Double getPctIntragenicBases() {
        return pctIntragenicBases;
    }

    public void setPctIntragenicBases(Double pctIntragenicBases) {
        this.pctIntragenicBases = pctIntragenicBases;
    }

    public Double getPctUtrBases() {
        return pctUtrBases;
    }

    public void setPctUtrBases(Double pctUtrBases) {
        this.pctUtrBases = pctUtrBases;
    }

    public Double getPctCodingBases() {
        return pctCodingBases;
    }

    public void setPctCodingBases(Double pctCodingBases) {
        this.pctCodingBases = pctCodingBases;
    }

    public Double getPctRiboBases() {
        return pctRiboBases;
    }

    public void setPctRiboBases(Double pctRiboBases) {
        this.pctRiboBases = pctRiboBases;
    }

    public Integer getUnmappedReads() {
        return unmappedReads;
    }

    public void setUnmappedReads(Integer unmappedReads) {
        this.unmappedReads = unmappedReads;
    }

    public Integer getReadPairDuplicates() {
        return readPairDuplicates;
    }

    public void setReadPairDuplicates(Integer readPairDuplicates) {
        this.readPairDuplicates = readPairDuplicates;
    }

    public Double getMskq() {
        return mskq;
    }

    public void setMskq(Double mskq) {
        this.mskq = mskq;
    }

    public Double getPctTarget100x() {
        return pctTarget100x;
    }

    public void setPctTarget100x(Double pctTarget100x) {
        this.pctTarget100x = pctTarget100x;
    }

    public Double getPctTarget80x() {
        return pctTarget80x;
    }

    public void setPctTarget80x(Double pctTarget80x) {
        this.pctTarget80x = pctTarget80x;
    }

    public Double getPctTarget40x() {
        return pctTarget40x;
    }

    public void setPctTarget40x(Double pctTarget40x) {
        this.pctTarget40x = pctTarget40x;
    }

    public Double getPctTarget30x() {
        return pctTarget30x;
    }

    public void setPctTarget30x(Double pctTarget30x) {
        this.pctTarget30x = pctTarget30x;
    }

    public Double getPctTarget10x() {
        return pctTarget10x;
    }

    public void setPctTarget10x(Double pctTarget10x) {
        this.pctTarget10x = pctTarget10x;
    }

    public Double getMeanTargetCoverage() {
        return meanTargetCoverage;
    }

    public void setMeanTargetCoverage(Double meanTargetCoverage) {
        this.meanTargetCoverage = meanTargetCoverage;
    }

    public Double getPctOffBait() {
        return pctOffBait;
    }

    public void setPctOffBait(Double pctOffBait) {
        this.pctOffBait = pctOffBait;
    }

    public Double getPctAdapters() {
        return pctAdapters;
    }

    public void setPctAdapters(Double pctAdapters) {
        this.pctAdapters = pctAdapters;
    }
}
