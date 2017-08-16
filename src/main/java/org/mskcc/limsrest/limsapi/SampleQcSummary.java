package org.mskcc.limsrest.limsapi;

import java.util.ArrayList;
import com.fasterxml.jackson.annotation.*;

public class SampleQcSummary {
   private String baitSet; 
   private String qcUnits;
   private String quantUnits;
   private double mskq;
   private double meanTargetCoverage;
   private double percentAdapters;
   private double percentDuplication;;
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
   public SampleQcSummary(){
        sampleName = "ERROR"; 
   }


   public void setBaitSet(String baitSet){
        this.baitSet = baitSet;
   }

   public void setQcUnits(String units){
       this.qcUnits = units;
   }

   public void setQuantUnits(String units){
        this.quantUnits = units;
   }

   public void setCreateDate(long createDate){
        this.createDate = createDate;
   }

   public void setMskq(double mskq){
        this.mskq = mskq ;
   }

   public void setMeanTargetCoverage(double mtc){
        this.meanTargetCoverage = mtc;
   }

   public void setPercentAdapters(double perAd){
        this.percentAdapters = perAd;
   }

   public void setPercentDuplication(double perDup){
        this.percentDuplication = perDup;
   }

   public void setPercentOffBait(double perOff){
        this.percentOffBait = perOff;
   }

   public void setPercentTarget10x(double per10x){
        this.percentTarget10x = per10x;
   }

   public void setPercentTarget30x(double per30x){
        this.percentTarget30x = per30x;
   }

   public void setPercentTarget100x(double per100x){
        this.percentTarget100x = per100x;
   }

   public void setPercentRibosomalBases(double perRibo){
        this.percentRibosomalBases = perRibo;
   }

   public void setPercentCodingBases(double perC){
        this.percentCodingBases = perC;
   }

   public void setPercentUtrBases(double perU){
        this.percentUtrBases = perU;
   }

   public void setPercentIntronicBases(double perI){
        this.percentIntronicBases = perI;
   }

   public void setPercentIntergenicBases(double perI){
        this.percentIntergenicBases = perI;
   }

   public void setPercentMrnaBases(double perM){
        this.percentMrnaBases = perM;
   }

   public void setQcControl(Double qcc){
        this.qcControl = qcc;
   }

   public void setQuantIt(Double quant){
        this.quantIt = quant;
   }
   public void setStartingAmount(double start){
        this.startingAmount = start;
   }

   public void setReadsDuped(long dup){
        this.readsDuped = dup;
   }

   public void setReadsExamined(long ex){
        this.readsExamined = ex;
   }

   public void setTotalReads(Long total){
       this.totalReads = total;
   }

   public void setUnmapped(long unmapped){
        this.unmappedReads = unmapped;
   }
   public void setUnpairedReadsExamined(long unpaired){
      this.unpairedExamined = unpaired;
   }

   public void setZeroCoveragePercent(double perZero){
        this.zeroCoveragePercent = perZero;
   }
   public void setRun(String run){
        this.run = run;
   }

   public void setSampleName(String sampleName){
        this.sampleName = sampleName;
   }

   public void setReviewed(boolean reviewed){
        this.reviewed = reviewed;
   }

   public void setQcStatus(String qcStatus){
        this.qcStatus = qcStatus;
   }
  
   public void setRecordId(long recordId){
      this.recordId = recordId;
   } 
 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getBaitSet(){
        return this.baitSet;
   }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getQcUnits(){
        return this.qcUnits;
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getQuantUnits(){
        return this.quantUnits;
  }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Long getCreateDate(){
       return this.createDate;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getMskq(){
        return this.mskq;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getMeanTargetCoverage(){
        return this.meanTargetCoverage;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getPercentAdapters(){
        return this.percentAdapters;
   }


 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getPercentDuplication(){
        return this.percentDuplication;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getPercentOffBait(){
        return this.percentOffBait;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getPercentTarget10x(){
        return this.percentTarget10x;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getPercentTarget30x(){
        return this.percentTarget30x;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getPercentTarget100x(){
        return this.percentTarget100x;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getPercentRibosomalBases(){
        return this.percentRibosomalBases;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getPercentCodingBases(){
        return this.percentCodingBases;
   }
 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getPercentIntronicBases(){
        return this.percentIntronicBases;
   }
 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getPercentIntergenicBases(){
        return this.percentIntergenicBases;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getPercentUtrBases(){
        return this.percentUtrBases;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getPercentMrnaBases(){
        return this.percentMrnaBases;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
 public Double getStartingAmount(){
        return this.startingAmount;
 }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
 public Double getQcControl(){
        return this.qcControl;
 }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
 public Double getQuantIt(){
        return this.quantIt;
 }


 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getReadsDuped(){
        return this.readsDuped;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getReadsExamined(){
        return this.readsExamined;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public Long getTotalReads(){
        return this.totalReads;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getUnmapped(){
        return this.unmappedReads;
   }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public long getUnpairedReadsExamined(){
      return this.unpairedExamined;
  }
 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getZeroCoveragePercent(){
        return this.zeroCoveragePercent;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getRun(){
        return this.run;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getSampleName(){
        return this.sampleName;
   }

 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public boolean getReviewed(){
        return this.reviewed;
   }


  @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public long getRecordId(){
        return this.recordId;
   }
 @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getQcStatus(){
        return this.qcStatus;
   }

  

}
