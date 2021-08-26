package org.mskcc.limsrest.service;

import java.util.List;
import java.util.LinkedList;
import com.fasterxml.jackson.annotation.*;
import lombok.Setter;

@Setter
public class RunSummary {
    private String awaitingSamples;
    private String concentrationUnits;
    private String runId;
    private String runLength;
    private String sampleId;
    private String requestName = "";
    private String otherSampleId;
    private String barcodeId = "";
    private String barcodeSeq = "";
    private String labHead = "";
    private String investigator = "";
    private String recipe = "";
    private String readNum;
    private long readTotal = 0;
    private long remainingReads = 0;
    private String requestId = "";
    private String runType = "";
    private String species = "";
    private String plateId;
    private String status = "";
    private String pool;
    private String tubeBarcode;
    private String tumorStatus;
    private String volume;
    private String wellPos;
    private Boolean fastqOnly;
    private Short numberRequestSamples;
    private int numberRequestedReads = 0;
    private long startDate = 0;
    private double concentration;
    private double altConcentration;
    private LinkedList<Long> lanes = new LinkedList<Long>();
    private long receivedDate = 0;

    public RunSummary(String runId, String sampleId) {
        this.runId = runId;
        this.sampleId = sampleId;
    }

    public void setAwaitingSamples(List<String> awaiting) {
        if (awaiting == null) {
            this.awaitingSamples = "";
        } else if (awaiting.size() > 0) {
            this.awaitingSamples = "Hold";
        } else {
            this.awaitingSamples = "May proceed";
        }
    }

    public void addLane(long lane){
     this.lanes.add(lane);
    }
    public String getRequestName() { return requestName; }

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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public long getRemainingReads() {
        return remainingReads;
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