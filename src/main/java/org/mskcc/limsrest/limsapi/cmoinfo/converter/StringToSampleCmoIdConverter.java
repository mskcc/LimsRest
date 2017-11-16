package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleId;

public interface StringToSampleCmoIdConverter {
    PatientCmoSampleId convert(CorrectedCmoSampleView correctedCmoSampleView);
}
