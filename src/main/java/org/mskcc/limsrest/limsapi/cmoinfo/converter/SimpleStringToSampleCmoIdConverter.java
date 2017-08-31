package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.mskcc.limsrest.limsapi.cmoinfo.PatientCmoSampleId;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleStringToSampleCmoIdConverter implements StringToSampleCmoIdConverter {
    @Override
    public PatientCmoSampleId convert(String sampleCmoIdString) {
        Pattern pattern = Pattern.compile("^C-(\\w+)-([NTRMLUPS])([0-9]){3}-([dr])$");
        Matcher matcher = pattern.matcher(sampleCmoIdString);

        if (!matcher.matches())
            throw new IllegalArgumentException(String.format("Sample cmo id: %s has incorrect format. Expected format is: %s",
                    sampleCmoIdString, pattern.toString()));

        String patientId = matcher.group(1);
        String sampleClass = matcher.group(2);
        int sampleCount = Integer.parseInt(matcher.group(3));
        String nucleicAcid = matcher.group(4);

        PatientCmoSampleId patientCmoSampleId = new PatientCmoSampleId(patientId, sampleClass, sampleCount, nucleicAcid);
        return patientCmoSampleId;
    }
}
