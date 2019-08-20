package org.mskcc.limsrest.service.cmoinfo.converter;

import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.service.cmoinfo.patientsample.PatientAwareCmoSampleId;

public interface CorrectedCmoSampleViewToSampleCmoIdConverter {
    PatientAwareCmoSampleId convert(CorrectedCmoSampleView correctedCmoSampleView);
}
