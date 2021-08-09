package org.mskcc.limsrest.service.sampletracker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class WESSampleData {

    String sampleId;
    String userSampleId;
    String userSampleidHistorical;
    String altId;
    String duplicateSample;
    String wesSampleid;
    String cmoSampleId;
    String cmoPatientId;
    String dmpSampleId;
    String dmpPatientId;
    String mrn;
    String sex;
    String sampleClass;
    String tumorType;
    String parentalTumorType;
    String tissueSite;
    String sourceDnaType;
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
    String sequencingSite;
    String piRequestDate;
    String tempoPipelineQcStatus;
    String tempoOutputDeliveryDate;
    String dataCustodian;
    String tissueType;
    String limsSampleRecordId;
    String limsTrackerRecordId;
}