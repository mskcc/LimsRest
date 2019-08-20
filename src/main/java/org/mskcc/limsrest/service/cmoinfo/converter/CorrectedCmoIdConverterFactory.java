package org.mskcc.limsrest.service.cmoinfo.converter;

public interface CorrectedCmoIdConverterFactory {
    CorrectedCmoSampleViewToSampleCmoIdConverter getConverter(String correctedCmoSampleId);
}
