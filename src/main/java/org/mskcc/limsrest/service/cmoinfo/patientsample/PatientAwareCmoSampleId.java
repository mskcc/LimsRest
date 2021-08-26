package org.mskcc.limsrest.service.cmoinfo.patientsample;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.mskcc.limsrest.service.cmoinfo.CmoSampleId;
import org.mskcc.limsrest.util.Constants;
import org.mskcc.limsrest.util.Utils;

import static com.google.common.base.Preconditions.checkArgument;

/***
 * PatientAwareCmoSampleId is a class which stores data needed to create CMO Sample Id for non-Cell line samples used
 * subsequently by Project Managers.
 */
@Getter @ToString @EqualsAndHashCode
public class PatientAwareCmoSampleId implements CmoSampleId {
    private final String patientId;
    private final String sampleTypeAbbr;
    private final int sampleCount;
    private final String nucleicAcid;

    /**
     * @param patientId      Anonymized patient id
     * @param sampleTypeAbbr Sample Type Abbreviation. String indicating sample Type (Specimen Type, Sample Origin,
     *                       Sample Class)
     * @param sampleCount    Sample Counter incremented for each sample for particular patient and @sampleTypeAbbr
     *                       across add the requests
     * @param nucleicAcid    Nucleid Acid Abrreviation. String indicating nucleid acid type.
     */
    public PatientAwareCmoSampleId(String patientId, String sampleTypeAbbr, int sampleCount, String nucleicAcid) {
        checkArgument(sampleCount >= Constants.SAMPLE_COUNT_MIN_VALUE && sampleCount <= Constants
                .SAMPLE_COUNT_MAX_VALUE, "Sample count value: %s is not in correct range <1-999>", sampleCount);
        Utils.requireNonNullNorEmpty(patientId, String.format("Patient id cannot be null nor empty"));
        Utils.requireNonNullNorEmpty(sampleTypeAbbr, String.format("Sample Type cannot be null nor empty"));
        Utils.requireNonNullNorEmpty(nucleicAcid, String.format("Nucleid acid cannot be null nor empty"));

        this.patientId = patientId;
        this.sampleTypeAbbr = sampleTypeAbbr;
        this.sampleCount = sampleCount;
        this.nucleicAcid = nucleicAcid;
    }
}
