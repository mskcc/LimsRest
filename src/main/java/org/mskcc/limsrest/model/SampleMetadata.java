package org.mskcc.limsrest.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @ToString
@Setter @AllArgsConstructor
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
}