package org.mskcc.limsrest.service;

/**
 * Additional fields necessary to generate a CMO Sample ID per the specification described:<BR>
 * http://plvpipetrack1.mskcc.org:8099/display/IDT/CMO+Patient+ID+and+Sample+ID+generation
 */
public class CMOSampleIdFields {
    private String naToExtract = "";
    private String normalizedPatientId = "";

    public CMOSampleIdFields() {
    }

    public CMOSampleIdFields(String naToExtract, String normalizedPatientId) {
        this.naToExtract = naToExtract;
        this.normalizedPatientId = normalizedPatientId;
    }

    public String getNaToExtract() {
        return naToExtract;
    }

    public void setNaToExtract(String naToExtract) {
        this.naToExtract = naToExtract;
    }

    public String getNormalizedPatientId() {
        return normalizedPatientId;
    }

    public void setNormalizedPatientId(String normalizedPatientId) {
        this.normalizedPatientId = normalizedPatientId;
    }

    @Override
    public String toString() {
        return "CMOSampleIdFields{" +
                "naToExtract='" + naToExtract + '\'' +
                ", normalizedPatientId='" + normalizedPatientId + '\'' +
                '}';
    }
}