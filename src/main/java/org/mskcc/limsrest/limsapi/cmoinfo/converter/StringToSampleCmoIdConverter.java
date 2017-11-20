package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientAwareCmoSampleId;

public interface StringToSampleCmoIdConverter {
    PatientAwareCmoSampleId convert(CorrectedCmoSampleView correctedCmoSampleView);
}
