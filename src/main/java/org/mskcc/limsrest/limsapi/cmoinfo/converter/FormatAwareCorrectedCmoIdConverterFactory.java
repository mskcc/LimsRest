package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.mskcc.limsrest.limsapi.cmoinfo.cspace.PatientAwareCorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.oldformat.OldCorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleAbbreviationRetriever;

public class FormatAwareCorrectedCmoIdConverterFactory implements CorrectedCmoIdConverterFactory {
    private final SampleAbbreviationRetriever sampleAbbreviationRetriever;

    public FormatAwareCorrectedCmoIdConverterFactory(SampleAbbreviationRetriever sampleAbbreviationRetriever) {
        this.sampleAbbreviationRetriever = sampleAbbreviationRetriever;
    }

    @Override
    public StringToSampleCmoIdConverter getConverter(String correctedCmoSampleId) {
        if (isInCspaceFormat(correctedCmoSampleId))
            return new PatientAwareCorrectedCmoIdConverter();

        if (isInOldFormat(correctedCmoSampleId))
            return new OldCorrectedCmoIdConverter(sampleAbbreviationRetriever);

        throw new UnsupportedCmoIdFormatException();
    }

    private boolean isInCspaceFormat(String correctedCmoSampleId) {
        return correctedCmoSampleId.matches(PatientAwareCorrectedCmoIdConverter.CMO_SAMPLE_ID_PATTERN);
    }

    private boolean isInOldFormat(String correctedCmoSampleId) {
        return correctedCmoSampleId.matches(OldCorrectedCmoIdConverter.OLD_CMO_SAMPLE_ID_PATTERN);
    }

    public static class UnsupportedCmoIdFormatException extends RuntimeException {

    }
}
