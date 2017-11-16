package org.mskcc.limsrest.limsapi.cmoinfo.converter;

public interface CorrectedCmoIdConverterFactory {
    StringToSampleCmoIdConverter getConverter(String correctedCmoSampleId);
}
