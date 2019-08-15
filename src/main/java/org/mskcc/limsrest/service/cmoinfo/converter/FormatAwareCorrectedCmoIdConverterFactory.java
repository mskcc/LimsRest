package org.mskcc.limsrest.service.cmoinfo.converter;

import org.mskcc.limsrest.service.cmoinfo.cspace.PatientAwareCorrectedCmoIdConverter;
import org.mskcc.limsrest.service.cmoinfo.cspace.StringCmoIdToCmoIdConverter;
import org.mskcc.limsrest.service.cmoinfo.oldformat.OldCorrectedCmoIdConverter;
import org.mskcc.limsrest.service.cmoinfo.retriever.SampleTypeAbbreviationRetriever;

public class FormatAwareCorrectedCmoIdConverterFactory implements CorrectedCmoIdConverterFactory {
    private final SampleTypeAbbreviationRetriever sampleTypeAbbreviationRetriever;

    public FormatAwareCorrectedCmoIdConverterFactory(SampleTypeAbbreviationRetriever sampleTypeAbbreviationRetriever) {
        this.sampleTypeAbbreviationRetriever = sampleTypeAbbreviationRetriever;
    }

    @Override
    public CorrectedCmoSampleViewToSampleCmoIdConverter getConverter(String correctedCmoSampleId) {
        if (isInCspaceFormat(correctedCmoSampleId))
            return new PatientAwareCorrectedCmoIdConverter();

        if (isInOldFormat(correctedCmoSampleId))
            return new OldCorrectedCmoIdConverter(sampleTypeAbbreviationRetriever);

        throw new UnsupportedCmoIdFormatException();
    }

    private boolean isInCspaceFormat(String correctedCmoSampleId) {
        return correctedCmoSampleId.matches(StringCmoIdToCmoIdConverter.CMO_SAMPLE_ID_PATTERN);
    }

    private boolean isInOldFormat(String correctedCmoSampleId) {
        return correctedCmoSampleId.matches(OldCorrectedCmoIdConverter.OLD_CMO_SAMPLE_ID_PATTERN);
    }

    public static class UnsupportedCmoIdFormatException extends RuntimeException {

    }
}
