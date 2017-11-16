package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.apache.commons.lang3.StringUtils;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.domain.sample.*;

public class BankedSampleToCorrectedCmoSampleIdConverter implements CorrectedCmoIdConverter<BankedSample> {
    @Override
    public CorrectedCmoSampleView convert(BankedSample bankedSample) {
        CorrectedCmoSampleView correctedCmoSampleView = new CorrectedCmoSampleView(bankedSample.getUserSampleID());

        correctedCmoSampleView.setSampleId(bankedSample.getUserSampleID());
        correctedCmoSampleView.setPatientId(bankedSample.getPatientId());
        correctedCmoSampleView.setRequestId(bankedSample.getRequestId());

        correctedCmoSampleView.setNucleidAcid(NucleicAcid.fromValue(bankedSample.getNAtoExtract()));
        correctedCmoSampleView.setSpecimenType(SpecimenType.fromValue(bankedSample.getSpecimenType()));

        if (!StringUtils.isEmpty(bankedSample.getSampleClass()))
            correctedCmoSampleView.setSampleClass(SampleClass.fromValue(bankedSample.getSampleClass()));

        if (!StringUtils.isEmpty(bankedSample.getSampleOrigin()))
            correctedCmoSampleView.setSampleOrigin(SampleOrigin.fromValue(bankedSample.getSampleOrigin()));

        return correctedCmoSampleView;
    }
}
