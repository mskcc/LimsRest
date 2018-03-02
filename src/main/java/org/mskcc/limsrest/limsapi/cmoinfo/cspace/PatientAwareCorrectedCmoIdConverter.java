package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.StringToSampleCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientAwareCmoSampleId;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatientAwareCorrectedCmoIdConverter implements StringToSampleCmoIdConverter {
    public static final String CMO_SAMPLE_ID_PATTERN = "^C-([a-zA-Z0-9]+)-([NTRMLUPSGX])([0-9]{3})-([dr])$";

    @Override
    public PatientAwareCmoSampleId convert(CorrectedCmoSampleView correctedCmoSampleView) {
        Pattern cSpaceCorrectedCmoPattern = Pattern.compile(CMO_SAMPLE_ID_PATTERN);
        Matcher cSpaceMatcher = cSpaceCorrectedCmoPattern.matcher(correctedCmoSampleView.getCorrectedCmoId());

        if (!cSpaceMatcher.matches())
            throw new IllegalArgumentException(String.format("Corrected cmo sample id: %s for sample: %s is not in " +
                            "expected format: %s",
                    correctedCmoSampleView.getCorrectedCmoId(), correctedCmoSampleView.getId(), CMO_SAMPLE_ID_PATTERN));

        String patientId = cSpaceMatcher.group(1);
        String sampleTypeAbbr = cSpaceMatcher.group(2);
        int sampleCount = Integer.parseInt(cSpaceMatcher.group(3));
        String nucleicAcid = cSpaceMatcher.group(4);

        PatientAwareCmoSampleId patientAwareCmoSampleId = new PatientAwareCmoSampleId(patientId, sampleTypeAbbr,
                sampleCount,
                nucleicAcid);
        return patientAwareCmoSampleId;
    }
}
