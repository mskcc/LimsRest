package org.mskcc.limsrest.service.dmp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
@Getter @Setter
public class DMPSample {
    private final String studySampleId;

    private Double concentration;
    private Double dnaInputIntoLibrary;
    private Double receivedDnaMass;
    private Double volume;
    private String barcodePlateId;
    private String collectionYear;
    private String dmpId;
    private String index;
    private String indexSequence;
    private String investigatorSampleId;
    private String nucleidAcidType;
    private String piName;
    private String preservation;
    private String sampleApprovedByCmo;
    private String sampleClass;
    private String sex;
    private String specimenType;
    private String studyOfTitle;
    private String trackingId;
    private String tumorType;
    private String wellPosition;

    public DMPSample(String studySampleId) {
        this.studySampleId = studySampleId;
    }
}
