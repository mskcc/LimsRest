package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientAwareCmoSampleId;

public interface CorrectedCmoSampleViewToSampleCmoIdConverter {
    PatientAwareCmoSampleId convert(CorrectedCmoSampleView correctedCmoSampleView);
}
