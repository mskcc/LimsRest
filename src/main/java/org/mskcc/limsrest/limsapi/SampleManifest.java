package org.mskcc.limsrest.limsapi;

public class SampleManifest {
    private String IGO_ID;
    private String CMO_PATIENT_ID;
    private String INVESTIGATOR_SAMPLE_ID;
    private String INVESTIGATOR_PATIENT_ID;
    private String ONCOTREE_CODE;
    private String SAMPLE_CLASS;
    private String TISSUE_SITE;
    private String SAMPLE_TYPE;
    private String SPECIMEN_PRESERVATION_TYPE;
    private String SPECIMEN_COLLECTION_YEAR;
    private String SEX;

    private String BARCODE_ID;
    private String BARCODE_INDEX_1;
    private String BARCODE_INDEX_2;
    private String LIBRARY_INPUT_NG; // [ng]
    private String LIBRARY_YIELD_NG;
    private String CAPTURE_INPUT_NG;
    private String CAPTURE_NAME;
    private String CAPTURE_CONCENTRATION_NM;
    private String CAPTURE_BAIT_SET;
    private String SPIKE_IN_GENES;
    private String STATUS;
    private String INCLUDE_RUN_ID;
    private String EXCLUDE_RUN_ID;
    // todo LANE_NUMBER;
    private String STUDY_ID;


    public SampleManifest() {}

    public String getIGO_ID() {
        return IGO_ID;
    }

    public void setIGO_ID(String IGO_ID) {
        this.IGO_ID = IGO_ID;
    }

    public String getCMO_PATIENT_ID() {
        return CMO_PATIENT_ID;
    }

    public void setCMO_PATIENT_ID(String CMO_PATIENT_ID) {
        this.CMO_PATIENT_ID = CMO_PATIENT_ID;
    }

    public String getINVESTIGATOR_SAMPLE_ID() {
        return INVESTIGATOR_SAMPLE_ID;
    }

    public void setINVESTIGATOR_SAMPLE_ID(String INVESTIGATOR_SAMPLE_ID) {
        this.INVESTIGATOR_SAMPLE_ID = INVESTIGATOR_SAMPLE_ID;
    }

    public String getINVESTIGATOR_PATIENT_ID() {
        return INVESTIGATOR_PATIENT_ID;
    }

    public void setINVESTIGATOR_PATIENT_ID(String INVESTIGATOR_PATIENT_ID) {
        this.INVESTIGATOR_PATIENT_ID = INVESTIGATOR_PATIENT_ID;
    }

    public String getONCOTREE_CODE() {
        return ONCOTREE_CODE;
    }

    public void setONCOTREE_CODE(String ONCOTREE_CODE) {
        this.ONCOTREE_CODE = ONCOTREE_CODE;
    }

    public String getSAMPLE_CLASS() {
        return SAMPLE_CLASS;
    }

    public void setSAMPLE_CLASS(String SAMPLE_CLASS) {
        this.SAMPLE_CLASS = SAMPLE_CLASS;
    }

    public String getTISSUE_SITE() {
        return TISSUE_SITE;
    }

    public void setTISSUE_SITE(String TISSUE_SITE) {
        this.TISSUE_SITE = TISSUE_SITE;
    }

    public String getSAMPLE_TYPE() {
        return SAMPLE_TYPE;
    }

    public void setSAMPLE_TYPE(String SAMPLE_TYPE) {
        this.SAMPLE_TYPE = SAMPLE_TYPE;
    }

    public String getSPECIMEN_PRESERVATION_TYPE() {
        return SPECIMEN_PRESERVATION_TYPE;
    }

    public void setSPECIMEN_PRESERVATION_TYPE(String SPECIMEN_PRESERVATION_TYPE) {
        this.SPECIMEN_PRESERVATION_TYPE = SPECIMEN_PRESERVATION_TYPE;
    }

    public String getSPECIMEN_COLLECTION_YEAR() {
        return SPECIMEN_COLLECTION_YEAR;
    }

    public void setSPECIMEN_COLLECTION_YEAR(String SPECIMEN_COLLECTION_YEAR) {
        this.SPECIMEN_COLLECTION_YEAR = SPECIMEN_COLLECTION_YEAR;
    }

    public String getSEX() {
        return SEX;
    }

    public void setSEX(String SEX) {
        this.SEX = SEX;
    }

    public String getBARCODE_ID() {
        return BARCODE_ID;
    }

    public void setBARCODE_ID(String BARCODE_ID) {
        this.BARCODE_ID = BARCODE_ID;
    }

    public String getBARCODE_INDEX_1() {
        return BARCODE_INDEX_1;
    }

    public void setBARCODE_INDEX_1(String BARCODE_INDEX_1) {
        this.BARCODE_INDEX_1 = BARCODE_INDEX_1;
    }

    public String getBARCODE_INDEX_2() {
        return BARCODE_INDEX_2;
    }

    public void setBARCODE_INDEX_2(String BARCODE_INDEX_2) {
        this.BARCODE_INDEX_2 = BARCODE_INDEX_2;
    }

    public String getLIBRARY_INPUT_NG() {
        return LIBRARY_INPUT_NG;
    }

    public void setLIBRARY_INPUT_NG(String LIBRARY_INPUT_NG) {
        this.LIBRARY_INPUT_NG = LIBRARY_INPUT_NG;
    }

    public String getLIBRARY_YIELD_NG() {
        return LIBRARY_YIELD_NG;
    }

    public void setLIBRARY_YIELD_NG(String LIBRARY_YIELD_NG) {
        this.LIBRARY_YIELD_NG = LIBRARY_YIELD_NG;
    }

    public String getCAPTURE_INPUT_NG() {
        return CAPTURE_INPUT_NG;
    }

    public void setCAPTURE_INPUT_NG(String CAPTURE_INPUT_NG) {
        this.CAPTURE_INPUT_NG = CAPTURE_INPUT_NG;
    }

    public String getCAPTURE_NAME() {
        return CAPTURE_NAME;
    }

    public void setCAPTURE_NAME(String CAPTURE_NAME) {
        this.CAPTURE_NAME = CAPTURE_NAME;
    }

    public String getCAPTURE_CONCENTRATION_NM() {
        return CAPTURE_CONCENTRATION_NM;
    }

    public void setCAPTURE_CONCENTRATION_NM(String CAPTURE_CONCENTRATION_NM) {
        this.CAPTURE_CONCENTRATION_NM = CAPTURE_CONCENTRATION_NM;
    }

    public String getCAPTURE_BAIT_SET() {
        return CAPTURE_BAIT_SET;
    }

    public void setCAPTURE_BAIT_SET(String CAPTURE_BAIT_SET) {
        this.CAPTURE_BAIT_SET = CAPTURE_BAIT_SET;
    }

    public String getSPIKE_IN_GENES() {
        return SPIKE_IN_GENES;
    }

    public void setSPIKE_IN_GENES(String SPIKE_IN_GENES) {
        this.SPIKE_IN_GENES = SPIKE_IN_GENES;
    }

    public String getSTATUS() {
        return STATUS;
    }

    public void setSTATUS(String STATUS) {
        this.STATUS = STATUS;
    }

    public String getINCLUDE_RUN_ID() {
        return INCLUDE_RUN_ID;
    }

    public void setINCLUDE_RUN_ID(String INCLUDE_RUN_ID) {
        this.INCLUDE_RUN_ID = INCLUDE_RUN_ID;
    }

    public String getEXCLUDE_RUN_ID() {
        return EXCLUDE_RUN_ID;
    }

    public void setEXCLUDE_RUN_ID(String EXCLUDE_RUN_ID) {
        this.EXCLUDE_RUN_ID = EXCLUDE_RUN_ID;
    }

    public String getSTUDY_ID() {
        return STUDY_ID;
    }

    public void setSTUDY_ID(String STUDY_ID) {
        this.STUDY_ID = STUDY_ID;
    }
}