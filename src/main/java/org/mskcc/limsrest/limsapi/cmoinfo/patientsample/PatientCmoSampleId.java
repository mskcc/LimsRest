package org.mskcc.limsrest.limsapi.cmoinfo.patientsample;

import org.mskcc.limsrest.limsapi.cmoinfo.CmoSampleId;
import org.mskcc.limsrest.staticstrings.Constants;
import org.mskcc.util.CommonUtils;

import static com.google.common.base.Preconditions.checkArgument;

public class PatientCmoSampleId implements CmoSampleId {
    private final String patientId;
    private final String sampleTypeAbbr;
    private final int sampleCount;
    private final String nucleicAcid;

    public PatientCmoSampleId(String patientId, String sampleTypeAbbr, int sampleCount, String nucleicAcid) {
        checkArgument(sampleCount >= Constants.SAMPLE_COUNT_MIN_VALUE && sampleCount <= Constants
                .SAMPLE_COUNT_MAX_VALUE, "Sample count value: %s is not in correct range <1-999>", sampleCount);
        CommonUtils.requireNonNullNorEmpty(patientId, String.format("Patient id cannot be null nor empty"));
        CommonUtils.requireNonNullNorEmpty(sampleTypeAbbr, String.format("Sample Type cannot be null nor empty"));
        CommonUtils.requireNonNullNorEmpty(nucleicAcid, String.format("Nucleid acid cannot be null nor empty"));

        this.patientId = patientId;
        this.sampleTypeAbbr = sampleTypeAbbr;
        this.sampleCount = sampleCount;
        this.nucleicAcid = nucleicAcid;
    }

    public int getSampleCount() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PatientCmoSampleId that = (PatientCmoSampleId) o;

        if (sampleCount != that.sampleCount) return false;
        if (patientId != null ? !patientId.equals(that.patientId) : that.patientId != null) return false;
        if (sampleTypeAbbr != null ? !sampleTypeAbbr.equals(that.sampleTypeAbbr) : that.sampleTypeAbbr != null)
            return false;
        return nucleicAcid != null ? nucleicAcid.equals(that.nucleicAcid) : that.nucleicAcid == null;
    }

    @Override
    public int hashCode() {
        int result = patientId != null ? patientId.hashCode() : 0;
        result = 31 * result + (sampleTypeAbbr != null ? sampleTypeAbbr.hashCode() : 0);
        result = 31 * result + (int) (sampleCount ^ (sampleCount >>> 32));
        result = 31 * result + (nucleicAcid != null ? nucleicAcid.hashCode() : 0);
        return result;
    }
}
