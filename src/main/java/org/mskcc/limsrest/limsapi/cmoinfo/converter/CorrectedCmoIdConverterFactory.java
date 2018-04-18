package org.mskcc.limsrest.limsapi.cmoinfo.converter;

public interface CorrectedCmoIdConverterFactory {
    CorrectedCmoSampleViewToSampleCmoIdConverter getConverter(String correctedCmoSampleId);
}
