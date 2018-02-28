package org.mskcc.limsrest.limsapi;

import java.util.ArrayList;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.*;

public class SampleSummary {
   private String id;
   private String cmoId;
   private String correctedCmoId;
   private String userId;
   private String assay;
   private String barcodeId;
   private String clinicalInfo;
   private String collectionYear;
   private String gender;
   private String geneticAlterations;
   private String investigator; 
   private String initialPool;
   private String preservation;
   private String specimenType;
   private String spikeInGenes;
   private String tissueType;
   private String patientId;
   private String platform;
   private String project;
   private String recipe;
   private SampleQcSummary qc;
   private LinkedList<BasicQc> basicQcs;
   private String organism;
   private String tubeId;
   private String tumorType;
   private String tumorOrNormal;
   private String sampleClass;
   private String sampleType;
   private String serviceId;
   private String runType;
   private String rowPosition;
   private String colPosition;
   private double concentration;
   private double yield;
   private double estimatedPurity;
   private double volume;
   private long dropOffDate;
   private Long readNumber;
   private Long recordId;
   private Integer coverage;
   private String concentrationUnits;
   private String plateId;
   public SampleSummary(){
        id = "ERROR"; 
        userId = ""; 
        project = "";
        recipe = ""; 
        organism = "";
        yield = 0;
        serviceId = "";
   }


  public void addBasicQc(BasicQc qc){
     if(basicQcs == null){
        basicQcs = new LinkedList<>();

     }
     basicQcs.add(qc);
  }

   public void addExpName(String userId){
        this.userId = userId;

   }


   public void addBaseId(String id){
        this.id = id ;

   }

   public void addCmoId(String otherId){
        this.cmoId = otherId;
   }

   public void addConcentration(double concentration){
       this.concentration = concentration;
   }

   public void addConcentrationUnits(String units){
        this.concentrationUnits = units;
   }


   public void addRequest(String proj){
        this.project = proj;

   }
   public void setRecipe(String recipe){
        this.recipe = recipe;

   }

   public void setAssay(String assay){ 
        this.assay = assay;
   }
  
   public void setBarcodeId(String barcodeId){
        this.barcodeId = barcodeId;
   }

   public void setClinicalInfo(String ci){
      this.clinicalInfo = ci;
   }     
  
   public void setCollectionYear(String year){
      this.collectionYear = year;
   }

   public void setCorrectedCmoId(String correctedCmoId){
        this.correctedCmoId = correctedCmoId;
   }

   public void setGeneticAlterations(String geneticAlterations){
      this.geneticAlterations = geneticAlterations;
   }

   public void setGender(String gender){
     this.gender = gender;
   }
   public void setInitialPool(String pool){
    this.initialPool = pool;
   }
   public void setInvestigator(String investigator){
    this.investigator = investigator;
   }
   public void setPatientId(String patientId){
    this.patientId = patientId;
   }

   public void setPreservation(String preservation){
     this.preservation = preservation;
   }

   public void setSampleClass(String sampleClass){
     this.sampleClass = sampleClass;
   }

   public void setSampleType(String sampleType){
     this.sampleType = sampleType;
   }

   public void setServiceId(String serviceId){
     this.serviceId = serviceId;
   }

   public void setSpecimenType(String specimenType){
     this.specimenType = specimenType;   
   }
   public void setSpikeInGenes(String spikeInGenes){
      this.spikeInGenes = spikeInGenes;
   }
   public void setTissueSite(String tissueType){
      this.tissueType = tissueType;
   }

   public void setTubeId(String tubeId){
      this.tubeId = tubeId;
   }

   public void addVolume(double volume){
      this.volume = volume;
   }

   public void setEstimatedPurity(double ep){
     this.estimatedPurity = ep;
   }

   public void setYield(double yield){
     this.yield = yield;
   }

  public void setQc(SampleQcSummary qc){
        this.qc = qc;
   }

  public void setPlatform(String platform){
     this.platform = platform;
  }

  public void setDropOffDate(long date){
    this.dropOffDate = date;
  }

  public void setRequestedReadNumber(long readNum){
    this.readNumber = readNum;
  }

  public void setCoverageTarget(int coverage){
     this.coverage = coverage;
  }

  public void setRunType(String runType){
     this.runType = runType;
  }

   public void setSpecies(String organism){
        this.organism = organism;
   }

  public void setTumorType(String tumorType){
    this.tumorType = tumorType;
  }

  public void setTumorOrNormal(String tumorOrNormal){
    this.tumorOrNormal = tumorOrNormal;
  }

  public void setPlateId(String plateId){
    this.plateId = plateId;
  }
  
  public void setColPosition(String colPos){
    this.colPosition = colPos;
  }

  public void setRowPosition(String rowPos){
    this.rowPosition = rowPos;
  }

  public void setRecordId(long recordId){
    this.recordId = Long.valueOf(recordId);
  }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getBaseId(){
        return id;
   }


    @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCmoId(){
        return cmoId;
   }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getConcentration(){
        return Double.toString(concentration) + " " + concentrationUnits;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getVolume(){
        return volume;
   }
   
   //Duplicate
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getVol(){
        return volume;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getEstimatedPurity(){
     return estimatedPurity;
   }


   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public double getYield(){
        return yield;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getPatientId(){
        return patientId;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getPlatform(){
        return platform;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getAssay(){
        return assay;
   }
   

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getBarcodeId(){
      return barcodeId;
   }
 
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getGeneticAlterations(){
      return geneticAlterations;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getKnownGeneticAlteration(){
      return geneticAlterations;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getGender(){
      return gender;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getInitialPool(){
      return initialPool;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getInvestigator(){
      return investigator;
   }
 
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getPreservation(){
     return preservation;
   }


   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getSampleClass(){
       return sampleClass;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getSampleType(){
       return sampleType;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getServiceId(){
     return serviceId;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getSpecimenType(){
     return specimenType;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getSpikeInGenes(){
      return spikeInGenes;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getTissueSite(){
      return tissueType;
   }

   //Duplicate
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getTissueType(){
       return tissueType;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getTubeId(){
        return tubeId;
   }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getClinicalInfo(){
        return clinicalInfo;
   }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCollectionYear(){
        return collectionYear;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getCorrectedCmoId(){
        return correctedCmoId;
   }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getPlateId(){
        return plateId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String  getColPosition(){
    return colPosition;
  }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getRowPosition(){
    return rowPosition;
  }


    @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getRecipe(){
    return recipe;
   }

   public String getCancerType(){
      if(tumorType == null){
        return tumorOrNormal;
      }
      return tumorType;
    }


    @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getTumorOrNormal(){
     return tumorOrNormal;
    }

   
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getProject(){
     return project;
    }

 
   public String getExpName(){
    return userId;
   }

     @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getUserId(){
        return userId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public SampleQcSummary getQc(){
    return qc;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public LinkedList<BasicQc> getBasicQcs(){
    return basicQcs;
   }


   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getSpecies(){
    return organism;
   }
   
   //duplicate
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getOrganism(){
        return organism;
    }

   public long getDropOffDate(){
     return dropOffDate;
   }
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public Long getRecordId(){
    return recordId;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public Long getRequestedNumberOfReads(){
     return readNumber;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public Integer getCoverageTarget(){
      return coverage;
   }

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String getRunType(){
     return runType;
   }

}
