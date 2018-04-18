package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.CorrectedCmoSampleViewToSampleCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientAwareCmoSampleId;

public class PatientAwareCorrectedCmoIdConverter implements CorrectedCmoSampleViewToSampleCmoIdConverter {

    private StringCmoIdToCmoIdConverter stringCmoIdToCmoIdConverter;

    @Override
    public PatientAwareCmoSampleId convert(CorrectedCmoSampleView correctedCmoSampleView) {
        stringCmoIdToCmoIdConverter = new StringCmoIdToCmoIdConverter();
        return stringCmoIdToCmoIdConverter.convert(correctedCmoSampleView.getCorrectedCmoId());
    }

}
