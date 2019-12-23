package org.mskcc.limsrest.service.cmoinfo.oldformat;

import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.service.cmoinfo.converter.CorrectedCmoSampleViewToSampleCmoIdConverter;
import org.mskcc.limsrest.service.cmoinfo.patientsample.PatientAwareCmoSampleId;
import org.mskcc.limsrest.service.cmoinfo.retriever.SampleTypeAbbreviationRetriever;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OldCorrectedCmoIdConverter implements CorrectedCmoSampleViewToSampleCmoIdConverter {
    public static final String OLD_CMO_SAMPLE_ID_PATTERN = "^.*-.*-.*-[a-zA-Z]{0,2}([0-9]+)[a-zA-Z]*$";

    private final SampleTypeAbbreviationRetriever sampleTypeAbbreviationRetriever;

    public OldCorrectedCmoIdConverter(SampleTypeAbbreviationRetriever sampleTypeAbbreviationRetriever) {
        this.sampleTypeAbbreviationRetriever = sampleTypeAbbreviationRetriever;
    }

    @Override
    public PatientAwareCmoSampleId convert(CorrectedCmoSampleView correctedCmoSampleView) {
        Pattern oldCorrectedCmoPattern = Pattern.compile(OLD_CMO_SAMPLE_ID_PATTERN);
        Matcher oldCorrectedCmoMatcher = oldCorrectedCmoPattern.matcher(correctedCmoSampleView.getCorrectedCmoId());

        if (!oldCorrectedCmoMatcher.matches())
            throw new RuntimeException(String.format("Corrected cmo sample id: %s for sample: %s is not in expected " +
                    "format: %s", correctedCmoSampleView.getCorrectedCmoId(), correctedCmoSampleView.getId(), OLD_CMO_SAMPLE_ID_PATTERN));

        int sampleCount = Integer.parseInt(oldCorrectedCmoMatcher.group(1));
        String patientId = correctedCmoSampleView.getPatientId();

        String sampleClass = sampleTypeAbbreviationRetriever.getSampleTypeAbbr(correctedCmoSampleView);
        String nucleicAcid = sampleTypeAbbreviationRetriever.getNucleicAcidAbbr(correctedCmoSampleView);

        PatientAwareCmoSampleId patientAwareCmoSampleId =
                new PatientAwareCmoSampleId(patientId, sampleClass, sampleCount, nucleicAcid);
        return patientAwareCmoSampleId;
    }
}
