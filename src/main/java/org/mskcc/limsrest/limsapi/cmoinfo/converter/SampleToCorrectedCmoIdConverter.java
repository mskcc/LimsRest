package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.apache.commons.lang3.StringUtils;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.domain.sample.*;
import org.mskcc.limsrest.limsapi.LimsException;

public class SampleToCorrectedCmoIdConverter implements CorrectedCmoIdConverter<Sample> {
    @Override
    public CorrectedCmoSampleView convert(Sample sample) throws LimsException {
        CorrectedCmoSampleView correctedCmoSampleView = new CorrectedCmoSampleView(sample.getIgoId());

        correctedCmoSampleView.setSampleId(sample.getCmoSampleInfo().getUserSampleID());
        correctedCmoSampleView.setNucleidAcid(NucleicAcid.fromValue(sample.getNAtoExtract()));
        correctedCmoSampleView.setPatientId(sample.getCmoSampleInfo().getCmoPatientId());
        correctedCmoSampleView.setRequestId(sample.getRequestId());

        if (!StringUtils.isEmpty(sample.getCorrectedSampleClass()))
            correctedCmoSampleView.setSampleClass(SampleClass.fromValue(sample.getCorrectedSampleClass()));

        if (!StringUtils.isEmpty(sample.getCorrectedCmoSampleOrigin()))
            correctedCmoSampleView.setSampleOrigin(SampleOrigin.fromValue(sample.getCorrectedCmoSampleOrigin()));

        if (!StringUtils.isEmpty(sample.getCorrectedSpecimenType()))
            correctedCmoSampleView.setSpecimenType(SpecimenType.fromValue(sample.getCorrectedSpecimenType()));

        if (!StringUtils.isEmpty(sample.getCorrectedCmoSampleId()))
            correctedCmoSampleView.setCorrectedCmoId(sample.getCorrectedCmoSampleId());

        return correctedCmoSampleView;
    }
}
