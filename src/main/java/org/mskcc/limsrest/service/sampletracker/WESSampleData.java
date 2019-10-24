package org.mskcc.limsrest.service.sampletracker;

public class WESSampleData {

    String sampleId;
    String userSampleId;
    String cmoSampleId;
    String cmoPatientId;
    String dmpSampleId;
    String dmpPatientId;
    String mrn;
    String sex;
    String sampleType;
    String sampleClass;
    String tumorType;
    String parentalTumorType;
    String tissueSite;
    String molecularAccessionNum;
    String collectionYear;
    String dateDmpRequest;
    String dmpRequestId;
    String igoRequestId;
    String dateIgoReceived;
    String igoCompleteDate;
    String applicationRequested;
    String baitsetUsed;
    String sequencerType;
    String projectTitle;
    String labHead;
    String ccFund;
    String scientificPi;
    Boolean consentPartAStatus;
    Boolean consentPartCStatus;
    String sampleStatus;
    String accessLevel;
    String clinicalTrial;
    String sequencingSite;
    String piRequestDate;
    String pipeline;
    String tissueType;
    String collaborationCenter;
    String limsSampleRecordId;
    String limsTrackerRecordId;

    public WESSampleData(String sampleId, String userSampleId, String cmoSampleId, String cmoPatientId, String dmpSampleId, String dmpPatientId, String mrn, String sex, String sampleType, String sampleClass, String tumorType,
                         String parentalTumorType, String tissueSite, String molecularAccessionNum, String collectionYear, String dateDmpRequest, String dmpRequestId, String igoRequestId, String dateIgoReceived, String igoCompleteDate,
                         String applicationRequested, String baitsetUsed, String sequencerType, String projectTitle, String labHead, String ccFund, String scientificPi, Boolean consentPartAStatus, Boolean consentPartCStatus,
                         String sampleStatus, String accessLevel, String clinicalTrial, String sequencingSite, String piRequestDate, String pipeline, String tissueType, String collaborationCenter, String limsSampleRecordId, String limsTrackerRecordId) {
        this.sampleId = sampleId;
        this.userSampleId = userSampleId;
        this.cmoSampleId = cmoSampleId;
        this.cmoPatientId = cmoPatientId;
        this.dmpSampleId = dmpSampleId;
        this.dmpPatientId = dmpPatientId;
        this.mrn = mrn;
        this.sex = sex;
        this.sampleType = sampleType;
        this.sampleClass = sampleClass;
        this.tumorType = tumorType;
        this.parentalTumorType = parentalTumorType;
        this.tissueSite = tissueSite;
        this.molecularAccessionNum = molecularAccessionNum;
        this.collectionYear = collectionYear;
        this.dateDmpRequest = dateDmpRequest;
        this.dmpRequestId = dmpRequestId;
        this.igoRequestId = igoRequestId;
        this.dateIgoReceived = dateIgoReceived;
        this.igoCompleteDate = igoCompleteDate;
        this.applicationRequested = applicationRequested;
        this.baitsetUsed = baitsetUsed;
        this.sequencerType = sequencerType;
        this.projectTitle = projectTitle;
        this.labHead = labHead;
        this.ccFund = ccFund;
        this.scientificPi = scientificPi;
        this.consentPartAStatus = consentPartAStatus;
        this.consentPartCStatus = consentPartCStatus;
        this.sampleStatus = sampleStatus;
        this.accessLevel = accessLevel;
        this.clinicalTrial = clinicalTrial;
        this.sequencingSite = sequencingSite;
        this.piRequestDate = piRequestDate;
        this.pipeline = pipeline;
        this.tissueType = tissueType;
        this.collaborationCenter = collaborationCenter;
        this.limsSampleRecordId = limsSampleRecordId;
        this.limsTrackerRecordId = limsTrackerRecordId;
    }
    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getUserSampleId() {
        return userSampleId;
    }

    public void setUserSampleId(String userSampleId) {
        this.userSampleId = userSampleId;
    }

    public String getCmoSampleId() {
        return cmoSampleId;
    }

    public void setCmoSampleId(String cmoSampleId) {
        this.cmoSampleId = cmoSampleId;
    }

    public String getCmoPatientId() {
        return cmoPatientId;
    }

    public void setCmoPatientId(String cmoPatientId) {
        this.cmoPatientId = cmoPatientId;
    }

    public String getDmpSampleId() {
        return dmpSampleId;
    }

    public void setDmpSampleId(String dmpSampleId) {
        this.dmpSampleId = dmpSampleId;
    }

    public String getDmpPatientId() {
        return dmpPatientId;
    }

    public void setDmpPatientId(String dmpPatientId) {
        this.dmpPatientId = dmpPatientId;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getSampleClass() {
        return sampleClass;
    }

    public void setSampleClass(String sampleClass) {
        this.sampleClass = sampleClass;
    }

    public String getTumorType() {
        return tumorType;
    }

    public void setTumorType(String tumorType) {
        this.tumorType = tumorType;
    }

    public String getParentalTumorType() {
        return parentalTumorType;
    }

    public void setParentalTumorType(String parentalTumorType) {
        this.parentalTumorType = parentalTumorType;
    }

    public String getTissueSite() {
        return tissueSite;
    }

    public void setTissueSite(String tissueSite) {
        this.tissueSite = tissueSite;
    }

    public String getMolecularAccessionNum() {
        return molecularAccessionNum;
    }

    public void setMolecularAccessionNum(String molecularAccessionNum) {
        this.molecularAccessionNum = molecularAccessionNum;
    }

    public String getCollectionYear() {
        return collectionYear;
    }

    public void setCollectionYear(String collectionYear) {
        this.collectionYear = collectionYear;
    }

    public String getDateDmpRequest() {
        return dateDmpRequest;
    }

    public void setDateDmpRequest(String dateDmpRequest) {
        this.dateDmpRequest = dateDmpRequest;
    }

    public String getDmpRequestId() {
        return dmpRequestId;
    }

    public void setDmpRequestId(String dmpRequestId) {
        this.dmpRequestId = dmpRequestId;
    }

    public String getIgoRequestId() {
        return igoRequestId;
    }

    public void setIgoRequestId(String igoRequestId) {
        this.igoRequestId = igoRequestId;
    }

    public String getDateIgoReceived() {
        return dateIgoReceived;
    }

    public void setDateIgoReceived(String dateIgoReceived) {
        this.dateIgoReceived = dateIgoReceived;
    }

    public String getIgoCompleteDate() {
        return igoCompleteDate;
    }

    public void setIgoCompleteDate(String igoCompleteDate) {
        this.igoCompleteDate = igoCompleteDate;
    }

    public String getApplicationRequested() {
        return applicationRequested;
    }

    public void setApplicationRequested(String applicationRequested) {
        this.applicationRequested = applicationRequested;
    }

    public String getBaitsetUsed() {
        return baitsetUsed;
    }

    public void setBaitsetUsed(String baitsetUsed) {
        this.baitsetUsed = baitsetUsed;
    }

    public String getSequencerType() {
        return sequencerType;
    }

    public void setSequencerType(String sequencerType) {
        this.sequencerType = sequencerType;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getLabHead() {
        return labHead;
    }

    public void setLabHead(String labHead) {
        this.labHead = labHead;
    }

    public String getCcFund() {
        return ccFund;
    }

    public void setCcFund(String ccFund) {
        this.ccFund = ccFund;
    }

    public String getScientificPi() {
        return scientificPi;
    }

    public void setScientificPi(String scientificPi) {
        this.scientificPi = scientificPi;
    }

    public Boolean getConsentPartAStatus() {
        return consentPartAStatus;
    }

    public void setConsentPartAStatus(Boolean consentPartAStatus) {
        this.consentPartAStatus = consentPartAStatus;
    }

    public Boolean getConsentPartCStatus() {
        return consentPartCStatus;
    }

    public void setConsentPartCStatus(Boolean consentPartCStatus) {
        this.consentPartCStatus = consentPartCStatus;
    }

    public String getSampleStatus() {
        return sampleStatus;
    }

    public void setSampleStatus(String sampleStatus) {
        this.sampleStatus = sampleStatus;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getClinicalTrial() {
        return clinicalTrial;
    }

    public void setClinicalTrial(String clinicalTrial) {
        this.clinicalTrial = clinicalTrial;
    }

    public String getSequencingSite() {
        return sequencingSite;
    }

    public void setSequencingSite(String sequencingSite) {
        this.sequencingSite = sequencingSite;
    }

    public String getPiRequestDate() {
        return piRequestDate;
    }

    public void setPiRequestDate(String piRequestDate) {
        this.piRequestDate = piRequestDate;
    }

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    public String getTissueType() {
        return tissueType;
    }

    public void setTissueType(String tissueType) {
        this.tissueType = tissueType;
    }

    public String getCollaborationCenter() {
        return collaborationCenter;
    }

    public void setCollaborationCenter(String collaborationCenter) {
        this.collaborationCenter = collaborationCenter;
    }

    public String getLimsSampleRecordId() {
        return limsSampleRecordId;
    }

    public void setLimsSampleRecordId(String limsRecordId) {
        this.limsSampleRecordId = limsRecordId;
    }

    public String getLimsTrackerRecordId() {
        return limsTrackerRecordId;
    }

    public void setLimsTrackerRecordId(String limsRecordId) {
        this.limsTrackerRecordId = limsTrackerRecordId;
    }
}