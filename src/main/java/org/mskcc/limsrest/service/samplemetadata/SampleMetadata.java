package org.mskcc.limsrest.service.samplemetadata;

public class SampleMetadata {
    private String mrn;
    private String cmoPatientId;
    private String cmoSampleId;
    private String igoId;
    private String investigatorSampleId;
    private String species;
    private String sex;
    private String tumorOrNormal;
    private String sampleType;
    private String preservation;
    private String tumorType;
    private String parentTumorType;
    private String specimenType;
    private String sampleOrigin;
    private String tissueSource;
    private String tissueLocation;
    private String recipe;
    private String baitset;
    private String fastqPath;
    String principalInvestigator;
    private String ancestorSample;
    private boolean doNotUse;
    private String sampleStatus;

    public SampleMetadata(String mrn, String cmoPatientId, String cmoSampleId, String igoId, String investigatorSampleId, String species,
                          String sex, String tumorOrNormal, String sampleType, String preservation, String tumorType, String parentTumorType,
                          String specimenType, String sampleOrigin, String tissueSource, String tissueLocation, String recipe,
                          String baitset, String fastqPath, String principalInvestigator, String ancestorSample, boolean doNotUse, String sampleStatus){
        this.mrn = mrn;
        this.cmoPatientId = cmoPatientId;
        this.cmoSampleId = cmoSampleId;
        this.igoId=igoId;
        this.investigatorSampleId=investigatorSampleId;
        this.species=species;
        this.sex=sex;
        this.tumorOrNormal=tumorOrNormal;
        this.sampleType=sampleType;
        this.preservation=preservation;
        this.tumorType=tumorType;
        this.parentTumorType=parentTumorType;
        this.specimenType=specimenType;
        this.sampleOrigin=sampleOrigin;
        this.tissueSource=tissueSource;
        this.tissueLocation=tissueLocation;
        this.recipe=recipe;
        this.baitset=baitset;
        this.principalInvestigator = principalInvestigator;
        this.fastqPath=fastqPath;
        this.ancestorSample=ancestorSample;
        this.doNotUse = doNotUse;
        this.sampleStatus = sampleStatus;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public String getCmoPatientId() {
        return cmoPatientId;
    }

    public void setCmoPatientId(String cmoPatientId) {
        this.cmoPatientId = cmoPatientId;
    }

    public String getCmoSampleId() {
        return cmoSampleId;
    }

    public void setCmoSampleId(String cmoSampleId) {
        this.cmoSampleId = cmoSampleId;
    }

    public String getIgoId() {
        return igoId;
    }

    public void setIgoId(String igoId) {
        this.igoId = igoId;
    }

    public String getInvestigatorSampleId() {
        return investigatorSampleId;
    }

    public void setInvestigatorSampleId(String investigatorSampleId) {
        this.investigatorSampleId = investigatorSampleId;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getTumorOrNormal() {
        return tumorOrNormal;
    }

    public void setTumorOrNormal(String tumorOrNormal) {
        this.tumorOrNormal = tumorOrNormal;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getPreservation() {
        return preservation;
    }

    public void setPreservation(String preservation) {
        this.preservation = preservation;
    }

    public String getTumorType() {
        return tumorType;
    }

    public void setTumorType(String tumorType) {
        this.tumorType = tumorType;
    }

    public String getParentTumorType() {
        return parentTumorType;
    }

    public void setParentTumorType(String parentTumorType) {
        this.parentTumorType = parentTumorType;
    }

    public String getSpecimenType() {
        return specimenType;
    }

    public void setSpecimenType(String specimenType) {
        this.specimenType = specimenType;
    }

    public String getSampleOrigin() {
        return sampleOrigin;
    }

    public void setSampleOrigin(String sampleOrigin) {
        this.sampleOrigin = sampleOrigin;
    }

    public String getTissueSource() {
        return tissueSource;
    }

    public void setTissueSource(String tissueSource) {
        this.tissueSource = tissueSource;
    }

    public String getTissueLocation() {
        return tissueLocation;
    }

    public void setTissueLocation(String tissueLocation) {
        this.tissueLocation = tissueLocation;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public String getBaitset() {
        return baitset;
    }

    public void setBaitset(String baitset) {
        this.baitset = baitset;
    }

    public String getFastqPath() {
        return fastqPath;
    }

    public void setFastqPath(String fastqPath) {
        this.fastqPath = fastqPath;
    }

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public void setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
    }

    public String getAncestorSample() {
        return ancestorSample;
    }

    public void setAncestorSample(String ancestorSample) {
        this.ancestorSample = ancestorSample;
    }

    public boolean isDoNotUse() {
        return doNotUse;
    }

    public void setDoNotUse(boolean doNotUse) {
        this.doNotUse = doNotUse;
    }

    public String getSampleStatus() {
        return sampleStatus;
    }

    public void setSampleStatus(String sampleStatus) {
        this.sampleStatus = sampleStatus;
    }

    @Override
    public String toString() {
        return "SampleMetadata{" +
                "mrn='" + mrn + '\'' +
                ", cmoPatientId='" + cmoPatientId + '\'' +
                ", cmoSampleId='" + cmoSampleId + '\'' +
                ", igoId='" + igoId + '\'' +
                ", investigatorSampleId='" + investigatorSampleId + '\'' +
                ", species='" + species + '\'' +
                ", sex='" + sex + '\'' +
                ", tumorOrNormal='" + tumorOrNormal + '\'' +
                ", sampleType='" + sampleType + '\'' +
                ", preservation='" + preservation + '\'' +
                ", tumorType='" + tumorType + '\'' +
                ", parentTumorType='" + parentTumorType + '\'' +
                ", specimenType='" + specimenType + '\'' +
                ", sampleOrigin='" + sampleOrigin + '\'' +
                ", tissueSource='" + tissueSource + '\'' +
                ", tissueLocation='" + tissueLocation + '\'' +
                ", recipe='" + recipe + '\'' +
                ", baitset='" + baitset + '\'' +
                ", fastqPath='" + fastqPath + '\'' +
                ", ancestorSample='" + ancestorSample + '\'' +
                ", doNotUse=" + doNotUse +
                ", sampleStatus='" + sampleStatus + '\'' +
                '}';
    }
}

