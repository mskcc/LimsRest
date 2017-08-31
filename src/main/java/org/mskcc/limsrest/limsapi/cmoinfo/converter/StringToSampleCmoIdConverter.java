package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.mskcc.limsrest.limsapi.cmoinfo.PatientCmoSampleId;

public interface StringToSampleCmoIdConverter {
    PatientCmoSampleId convert(String sampleCmoIdString);
}
