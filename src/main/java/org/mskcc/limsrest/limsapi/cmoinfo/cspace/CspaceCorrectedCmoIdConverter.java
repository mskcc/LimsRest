package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.StringToSampleCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleId;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CspaceCorrectedCmoIdConverter implements StringToSampleCmoIdConverter {
    public static final String PATTERN = "^C-([a-zA-Z0-9]+)-([NTRMLUPSGX])([0-9]{3})-([dr])$";

    @Override
    public PatientCmoSampleId convert(CorrectedCmoSampleView correctedCmoSampleView) {
        Pattern cSpaceCorrectedCmoPattern = Pattern.compile(PATTERN);
        Matcher cSpaceMatcher = cSpaceCorrectedCmoPattern.matcher(correctedCmoSampleView.getCorrectedCmoId());

        if (!cSpaceMatcher.matches())
            throw new IllegalArgumentException(String.format("Corrected cmo sample id: %s for sample: %s is not in " +
                            "expected format: %s",
                    correctedCmoSampleView.getCorrectedCmoId(), correctedCmoSampleView.getId(), PATTERN));

        String patientId = cSpaceMatcher.group(1);
        String sampleTypeAbbr = cSpaceMatcher.group(2);
        int sampleCount = Integer.parseInt(cSpaceMatcher.group(3));
        String nucleicAcid = cSpaceMatcher.group(4);

        PatientCmoSampleId patientCmoSampleId = new PatientCmoSampleId(patientId, sampleTypeAbbr, sampleCount,
                nucleicAcid);
        return patientCmoSampleId;
    }
}
