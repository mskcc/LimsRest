package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.domain.sample.*;
import org.mskcc.limsrest.limsapi.LimsException;
import org.mskcc.limsrest.limsapi.PatientSamplesWithCmoInfoRetriever;

public class SampleToCorrectedCmoIdConverter implements CorrectedCmoIdConverter<Sample> {
    private final static Log LOGGER = LogFactory.getLog(PatientSamplesWithCmoInfoRetriever.class);

    @Override
    public CorrectedCmoSampleView convert(Sample sample) throws LimsException {
        LOGGER.debug(String.format("Converting sample %s to Corrected Cmo Sample View", sample));

        try {
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

            LOGGER.debug(String.format("Converted sample %s to Corrected Cmo Sample View: %s", sample,
                    correctedCmoSampleView));

            return correctedCmoSampleView;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error while retrieving information for sample: %s. Couse: %s",
                    sample.getIgoId(), e.getMessage()));
        }
    }
}
