package org.mskcc.limsrest.limsapi.cmoinfo;

import static com.google.common.base.Preconditions.checkArgument;

public class PatientCmoSampleId implements CmoSampleId {
    private final String patientId;
    private final String sampleTypeAbbr;
    private final long sampleCount;
    private final String nucleicAcid;

    public PatientCmoSampleId(String patientId, String sampleTypeAbbr, long sampleCount, String nucleicAcid) {
        checkArgument(sampleCount > 0 && sampleCount <= 999, "Sample count value: %s is not in correct range <1-999>", sampleCount);

        this.patientId = patientId;
        this.sampleTypeAbbr = sampleTypeAbbr;
        this.sampleCount = sampleCount;
        this.nucleicAcid = nucleicAcid;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getSampleTypeAbbr() {
        return sampleTypeAbbr;
    }

    public String getNucleicAcid() {
        return nucleicAcid;
    }
}
