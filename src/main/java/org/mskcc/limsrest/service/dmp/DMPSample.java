package org.mskcc.limsrest.service.dmp;

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

    public String getStudySampleId() {
        return studySampleId;
    }

    public String getTumorType() {
        return tumorType;
    }

    public void setTumorType(String tumorType) {
        this.tumorType = tumorType;
    }

    public String getNucleidAcidType() {
        return nucleidAcidType;
    }

    public void setNucleidAcidType(String nucleidAcidType) {
        this.nucleidAcidType = nucleidAcidType;
    }

    public String getPiName() {
        return piName;
    }

    public void setPiName(String piName) {
        this.piName = piName;
    }

    public String getCollectionYear() {
        return collectionYear;
    }

    public void setCollectionYear(String collectionYear) {
        this.collectionYear = collectionYear;
    }

    public String getPreservation() {
        return preservation;
    }

    public void setPreservation(String preservation) {
        this.preservation = preservation;
    }

    public String getIndexSequence() {
        return indexSequence;
    }

    public void setIndexSequence(String indexSequence) {
        this.indexSequence = indexSequence;
    }

    public String getSpecimenType() {
        return specimenType;
    }

    public void setSpecimenType(String specimenType) {
        this.specimenType = specimenType;
    }

    public String getSampleClass() {
        return sampleClass;
    }

    public void setSampleClass(String sampleClass) {
        this.sampleClass = sampleClass;
    }

    public String getStudyOfTitle() {
        return studyOfTitle;
    }

    public void setStudyOfTitle(String studyOfTitle) {
        this.studyOfTitle = studyOfTitle;
    }

    public String getDmpId() {
        return dmpId;
    }

    public void setDmpId(String dmpId) {
        this.dmpId = dmpId;
    }

    public String getBarcodePlateId() {
        return barcodePlateId;
    }

    public void setBarcodePlateId(String barcodePlateId) {
        this.barcodePlateId = barcodePlateId;
    }

    public String getInvestigatorSampleId() {
        return investigatorSampleId;
    }

    public void setInvestigatorSampleId(String investigatorSampleId) {
        this.investigatorSampleId = investigatorSampleId;
    }

    public String getWellPosition() {
        return wellPosition;
    }

    public void setWellPosition(String wellPosition) {
        this.wellPosition = wellPosition;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public Double getConcentration() {
        return concentration;
    }

    public void setConcentration(Double concentration) {
        this.concentration = concentration;
    }

    public String getSampleApprovedByCmo() {
        return sampleApprovedByCmo;
    }

    public void setSampleApprovedByCmo(String sampleApprovedByCmo) {
        this.sampleApprovedByCmo = sampleApprovedByCmo;
    }

    public Double getReceivedDnaMass() {
        return receivedDnaMass;
    }

    public void setReceivedDnaMass(Double receivedDnaMass) {
        this.receivedDnaMass = receivedDnaMass;
    }

    public Double getDnaInputIntoLibrary() {
        return dnaInputIntoLibrary;
    }

    public void setDnaInputIntoLibrary(Double dnaInputIntoLibrary) {
        this.dnaInputIntoLibrary = dnaInputIntoLibrary;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
}
