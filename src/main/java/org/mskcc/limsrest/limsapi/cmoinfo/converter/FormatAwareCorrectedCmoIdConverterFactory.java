package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.mskcc.limsrest.limsapi.cmoinfo.cspace.CspaceCorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.noformat.NoFormatCorrectedCmoIdConverter;
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
            return new CspaceCorrectedCmoIdConverter();

        if (isInOldFormat(correctedCmoSampleId))
            return new OldCorrectedCmoIdConverter(sampleAbbreviationRetriever);

        return new NoFormatCorrectedCmoIdConverter(sampleAbbreviationRetriever);
    }

    private boolean isInCspaceFormat(String correctedCmoSampleId) {
        return correctedCmoSampleId.matches(CspaceCorrectedCmoIdConverter.PATTERN);
    }

    private boolean isInOldFormat(String correctedCmoSampleId) {
        return correctedCmoSampleId.matches(OldCorrectedCmoIdConverter.PATTERN);
    }
}
