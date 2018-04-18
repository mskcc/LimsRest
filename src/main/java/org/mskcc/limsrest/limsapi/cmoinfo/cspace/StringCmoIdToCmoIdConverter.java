package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientAwareCmoSampleId;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringCmoIdToCmoIdConverter {
    public static final String CMO_SAMPLE_ID_PATTERN = "^C-([a-zA-Z0-9]+)-([NTRMLUPSGX])([0-9]{3})-([dr])$";
    private final static Log LOGGER = LogFactory.getLog(StringCmoIdToCmoIdConverter.class);

    public PatientAwareCmoSampleId convert(String cmoSampleId) {
        Pattern cSpaceCorrectedCmoPattern = Pattern.compile(CMO_SAMPLE_ID_PATTERN);
        Matcher cSpaceMatcher = cSpaceCorrectedCmoPattern.matcher(cmoSampleId);

        if (!cSpaceMatcher.matches())
            throw new IllegalArgumentException(String.format("Corrected cmo sample id: %s is not in " +
                            "expected format: %s",
                    cmoSampleId, CMO_SAMPLE_ID_PATTERN));

        String patientId = cSpaceMatcher.group(1);
        String sampleTypeAbbr = cSpaceMatcher.group(2);
        int sampleCount = Integer.parseInt(cSpaceMatcher.group(3));
        String nucleicAcid = cSpaceMatcher.group(4);

        PatientAwareCmoSampleId patientAwareCmoSampleId = new PatientAwareCmoSampleId(patientId, sampleTypeAbbr,
                sampleCount,
                nucleicAcid);

        LOGGER.info(String.format("Converted cmo sample id: %s to cmo sample id object %s", cmoSampleId,
                patientAwareCmoSampleId));

        return patientAwareCmoSampleId;
    }
}
