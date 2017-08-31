package org.mskcc.domain;

import java.util.HashMap;
import java.util.Map;

public class BankedSample {
    public static final String ASSAY = "Assay";
    public static final String SAMPLE_CLASS = "SampleClass";
    public static final String BARCODE_ID = "BarcodeId";
    public static final String CLINICAL_INFO = "ClinicalInfo";
    public static final String COL_POSITION = "ColPosition";
    public static final String COLLECTION_YEAR = "CollectionYear";
    public static final String CONCENTRATION = "Concentration";
    public static final String CONCENTRATION_UNITS = "ConcentrationUnits";
    public static final String CREATED_BY = "CreatedBy";
    public static final String DATA_RECORD_NAME = "DataRecordName";
    public static final String DATA_TYPE_NAME = "BankedSample";
    public static final String DATE_CREATED = "DateCreated";
    public static final String ESTIMATED_PURITY = "EstimatedPurity";
    public static final String GENDER = "Gender";
    public static final String GENETIC_ALTERATIONS = "GeneticAlterations";
    public static final String INVESTIGATOR = "Investigator";
    public static final String NATO_EXTRACT = "NAtoExtract";
    public static final String ORGANISM = "Organism";
    public static final String OTHER_SAMPLE_ID = "OtherSampleId";
    public static final String PATIENT_ID = "PatientId";
    public static final String PLATFORM = "Platform";
    public static final String PRESERVATION = "Preservation";
    public static final String PROMOTED = "Promoted";
    public static final String RECIPE = "Recipe";
    public static final String RECORD_ID = "RecordId";
    public static final String REQUEST_ID = "RequestId";
    public static final String REQUESTED_READS = "RequestedReads";
    public static final String ROW_POSITION = "RowPosition";
    public static final String RUN_TYPE = "RunType";
    public static final String SAMPLE_ORIGIN = "SampleOrigin";
    public static final String SERVICE_ID = "ServiceId";
    public static final String SPECIMEN_TYPE = "SpecimenType";
    public static final String SPIKE_IN_GENES = "SpikeInGenes";
    public static final String TISSUE_SITE = "TissueSite";
    public static final String TUMOR_OR_NORMAL = "TumorOrNormal";
    public static final String TUMOR_TYPE = "TumorType";
    public static final String USER_SAMPLE_ID = "UserSampleID";
    public static final String VOLUME = "Volume";
    private Map<String, Object> fields = new HashMap<>();

    public BankedSample() {
    }

    public BankedSample(Map<String, Object> fields) {
        this.fields = fields;
    }

    public String getOtherSampleId() {
        return (String) fields.get("OtherSampleId");
    }

    public void setOtherSampleId(String value) {
        fields.put("OtherSampleId", value);
    }

    public String getRowPosition() {
        return (String) fields.get("RowPosition");
    }

    public void setRowPosition(String value) {
        fields.put("RowPosition", value);
    }

    public String getColPosition() {
        return (String) fields.get("ColPosition");
    }

    public void setColPosition(String value) {
        fields.put("ColPosition", value);
    }

    public String getRunType() {
        return (String) fields.get("RunType");
    }

    public void setRunType(String value) {
        fields.put("RunType", value);
    }

    public String getRecipe() {
        return (String) fields.get("Recipe");
    }

    public void setRecipe(String value) {
        fields.put("Recipe", value);
    }

    public Double getRequestedReads() {
        return (Double) fields.get("RequestedReads");
    }

    public void setRequestedReads(Double value) {
        fields.put("RequestedReads", value);
    }

    public String getInvestigator() {
        return (String) fields.get("Investigator");
    }

    public void setInvestigator(String value) {
        fields.put("Investigator", value);
    }

    public String getBarcodeId() {
        return (String) fields.get("BarcodeId");
    }

    public void setBarcodeId(String value) {
        fields.put("BarcodeId", value);
    }

    public Boolean getPromoted() {
        return (Boolean) fields.get("Promoted");
    }

    public void setPromoted(Boolean value) {
        fields.put("Promoted", value);
    }

    public String getServiceId() {
        return (String) fields.get("ServiceId");
    }

    public void setServiceId(String value) {
        fields.put("ServiceId", value);
    }

    public String getTissueSite() {
        return (String) fields.get("TissueSite");
    }

    public void setTissueSite(String value) {
        fields.put("TissueSite", value);
    }

    public String getSpikeInGenes() {
        return (String) fields.get("SpikeInGenes");
    }

    public void setSpikeInGenes(String value) {
        fields.put("SpikeInGenes", value);
    }

    public String getSpecimenType() {
        return (String) fields.get("SpecimenType");
    }

    public void setSpecimenType(String value) {
        fields.put("SpecimenType", value);
    }

    public String getPreservation() {
        return (String) fields.get("Preservation");
    }

    public void setPreservation(String value) {
        fields.put("Preservation", value);
    }

    public String getPlatform() {
        return (String) fields.get("Platform");
    }

    public void setPlatform(String value) {
        fields.put("Platform", value);
    }

    public String getGeneticAlterations() {
        return (String) fields.get("GeneticAlterations");
    }

    public void setGeneticAlterations(String value) {
        fields.put("GeneticAlterations", value);
    }

    public String getGender() {
        return (String) fields.get("Gender");
    }

    public void setGender(String value) {
        fields.put("Gender", value);
    }

    public Double getEstimatedPurity() {
        return (Double) fields.get("EstimatedPurity");
    }

    public void setEstimatedPurity(Double value) {
        fields.put("EstimatedPurity", value);
    }

    public String getConcentrationUnits() {
        return (String) fields.get("ConcentrationUnits");
    }

    public void setConcentrationUnits(String value) {
        fields.put("ConcentrationUnits", value);
    }

    public Double getConcentration() {
        return (Double) fields.get("Concentration");
    }

    public void setConcentration(Double value) {
        fields.put("Concentration", value);
    }

    public String getCollectionYear() {
        return (String) fields.get("CollectionYear");
    }

    public void setCollectionYear(String value) {
        fields.put("CollectionYear", value);
    }

    public String getClinicalInfo() {
        return (String) fields.get("ClinicalInfo");
    }

    public void setClinicalInfo(String value) {
        fields.put("ClinicalInfo", value);
    }

    public String getAssay() {
        return (String) fields.get("Assay");
    }

    public void setAssay(String value) {
        fields.put("Assay", value);
    }

    public String getRequestId() {
        return (String) fields.get("RequestId");
    }

    public void setRequestId(String value) {
        fields.put("RequestId", value);
    }

    public Double getVolume() {
        return (Double) fields.get("Volume");
    }

    public void setVolume(Double value) {
        fields.put("Volume", value);
    }

    public String getTumorOrNormal() {
        return (String) fields.get("TumorOrNormal");
    }

    public void setTumorOrNormal(String value) {
        fields.put("TumorOrNormal", value);
    }

    public String getTumorType() {
        return (String) fields.get("TumorType");
    }

    public void setTumorType(String value) {
        fields.put("TumorType", value);
    }

    public String getOrganism() {
        return (String) fields.get("Organism");
    }

    public void setOrganism(String value) {
        fields.put("Organism", value);
    }

    public String getDataRecordName() {
        return (String) fields.get("DataRecordName");
    }

    public String getCreatedBy() {
        return (String) fields.get("CreatedBy");
    }

    public Long getDateCreated() {
        return (Long) fields.get("DateCreated");
    }

    public String getPatientId() {
        return (String) fields.get(PATIENT_ID);
    }

    public void setPatientId(String patientId) {
        fields.put(PATIENT_ID, patientId);
    }

    public String getSampleClass() {
        return (String) fields.get(SAMPLE_CLASS);
    }

    public void setSampleClass(String value) {
        fields.put(SAMPLE_CLASS, value);
    }

    public String getNucleicAcidType() {
        return (String) fields.get(NATO_EXTRACT);
    }

    public void setNucleicAcidType(String value) {
        fields.put(NATO_EXTRACT, value);
    }

    public String getSampleOrigin() {
        return (String) fields.get(SAMPLE_ORIGIN);
    }

    public void setSampleOrigin(String value) {
        fields.put(SAMPLE_ORIGIN, value);
    }

    public String getUserSampleId() {
        return (String) fields.get(USER_SAMPLE_ID);
    }

    public void setUserSampleId(String value) {
        fields.put(USER_SAMPLE_ID, value);
    }

    public long getRecordId() {
        return (long) fields.get(RECORD_ID);
    }
}
