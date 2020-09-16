package org.mskcc.limsrest.service;

import java.util.List;
import java.util.LinkedList;
import com.fasterxml.jackson.annotation.*;

public class RunSummary{

   private String awaitingSamples; 
   private String batch;
   private String concentrationUnits;
   private String runId;
   private String runLength;
   private String sampleId;
   private String otherSampleId;
   private String barcodeId;
   private String barcodeSeq;
   private String labHead;
   private String investigator;
   private String recipe;
   private String readNum;
   private long readTotal;
   private long remainingReads;
   private String requestId;
   private String runType;
   private String sequencer;
   private String species;
   private String plateId;
   private String status;
   private String pool;
   private String tubeBarcode;
   private String tumorStatus;
   private String volume;
   private String wellPos;
   private Boolean fastqOnly;
   private Short numberRequestSamples;

   private int numberRequestedReads;
   private long startDate;
   private double concentration;
   private double altConcentration;
   private LinkedList<Long> lanes;
   private long receivedDate;

   public RunSummary(String runId, String sampleId){
        this.runId = runId;
        this.sampleId = sampleId; 
        barcodeId = "";
        barcodeSeq = "";
        labHead = "";
        investigator = "";
        recipe = ""; 
        requestId = "";
        runType = "";
        species = "";
        status = "";
        numberRequestedReads = 0;
        readTotal = 0;
        remainingReads = 0;
        startDate = 0;
        lanes = new LinkedList<Long>();
	    receivedDate = 0;
   
   }


  public void setAwaitingSamples(List<String> awaiting){
    if(awaiting == null){
        this.awaitingSamples = "";
    }
    else if(awaiting.size() > 0){
        this.awaitingSamples = "Hold";
    } else{
        this.awaitingSamples = "May proceed";
    }
  }

    public void setSampleId(String sampleId) {this.sampleId = sampleId;}
    public void setBarcodeId(String bcid){
    this.barcodeId = bcid;
    }
    public void setBarcodeSeq(String bcSeq){
    this.barcodeSeq = bcSeq;
    }

    public void setBatch(String batch){
    this.batch = batch;
    }

    public void setLabHead(String labHead){
    this.labHead = labHead;
    }
    public void setInvestigator(String investigator){
    this.investigator = investigator;
    }
    public void setRecipe(String recipe){
    this.recipe = recipe;
    }
    public void setRequestId(String requestId){
    this.requestId = requestId;
    }
    public void setReadNum(String readNum){
    this.readNum = readNum;
    }
    public void setRunType(String runType){
    this.runType = runType;
    }
    public void setOtherSampleId(String otherSampleId){
    this.otherSampleId = otherSampleId;
    }
    public void setSequencer(String seq){
      this.sequencer = seq;
    }


    public void setSpecies(String species){
        this.species = species;
    }

    public void setStatus(String status){
       this.status = status;
    }
    public void setNumberRequestSamples(Short num){
      this.numberRequestSamples = num;
    }

    public void setNumberRequestedReads(int numberReqReads){
    this.numberRequestedReads = numberReqReads;
    }

    public void setStartDate(long creationInEpochMilli){
    this.startDate = creationInEpochMilli;
    }

    public void setReceivedDate(long creationInEpochMilli){
    this.receivedDate = creationInEpochMilli;
    }

    public void addLane(long lane){
     this.lanes.add(lane);
    }

    public void setPlateId(String plate){
     this.plateId = plate;
    }

    public void setPool(String pool){
    this.pool = pool;
    }

    public void setConcentration(double concentration){
     this.concentration = concentration;
    }


    public void setAltConcentration(double concentration){
     this.altConcentration = concentration;
    }

    public void setConcentrationUnits(String units){
     this.concentrationUnits = units;
    }

    public void setTumor(String status){
    this.tumorStatus = status;
    }

    public void setTubeBarcode(String tubeBarcode){
    this.tubeBarcode = tubeBarcode;
    }

    public void setVolume(String volume){
    this.volume = volume;
    }

    public void setWellPos(String wellPos){
    this.wellPos = wellPos;
    }

    public void setFastqOnly(Boolean fastq){
     this.fastqOnly = fastq;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getAwaitingSamples(){
    return awaitingSamples;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRunId(){
    return runId;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleId(){
    return sampleId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getStatus(){
     return status;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getOtherSampleId(){
    return otherSampleId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getBarcodeId(){
    return barcodeId;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getBarcodeSeq(){
    return barcodeSeq;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getBatch(){
    return batch;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getLabHead(){
    return labHead;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getInvestigator(){
    return investigator;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRecipe(){
    return recipe;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRequestId(){
    return requestId;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getReadNum(){
    return readNum;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRunType(){
    return runType;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSpecies(){
        return species;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public int getNumberRequestedReads(){
    return numberRequestedReads;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getStartDate(){
     return startDate;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getReceivedDate(){
     return receivedDate;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPlateId(){
    return plateId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPool(){
    return pool;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSequencer(){
     return sequencer;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getTumor(){
    return tumorStatus;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getTubeBarcode(){
     return tubeBarcode;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getVolume(){
     return volume;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getWellPos(){
     return wellPos;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Boolean getFastqOnly(){
    return fastqOnly;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Short getNumberRequestSamples(){
    return numberRequestSamples;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getConcentration(){
    return concentration;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public double getAltConcentration(){
    return altConcentration;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getReadTotal() {
        return readTotal;
    }

    public void setReadTotal(long readTotal) {
        this.readTotal = readTotal;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getRemainingReads() {
        return remainingReads;
    }

    public void setRemainingReads(long remainingReads) {
        this.remainingReads = remainingReads;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getConcentrationUnits(){
    return concentrationUnits;
    }
    public LinkedList<Long> getLanes(){
         return lanes;
    }
    public static RunSummary errorMessage(String e, String trace){
        RunSummary rs = new RunSummary("ERROR: " + e, trace);
    return rs;
    }
}
