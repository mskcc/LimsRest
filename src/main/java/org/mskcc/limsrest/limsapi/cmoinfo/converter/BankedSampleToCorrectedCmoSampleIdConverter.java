package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.domain.sample.*;
import org.mskcc.limsrest.limsapi.PatientSamplesWithCmoInfoRetriever;

public class BankedSampleToCorrectedCmoSampleIdConverter implements CorrectedCmoIdConverter<BankedSample> {
    private final static Log LOGGER = LogFactory.getLog(PatientSamplesWithCmoInfoRetriever.class);

    @Override
    public CorrectedCmoSampleView convert(BankedSample bankedSample) {
        LOGGER.debug(String.format("Converting sample %s to Corrected Cmo Sample View", bankedSample));

        try {
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

            LOGGER.debug(String.format("Converted sample %s to Corrected Cmo Sample View: %s", bankedSample,
                    correctedCmoSampleView));

            return correctedCmoSampleView;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error while retrieving information for sample: %s. Couse: %s",
                    bankedSample.getId(), e.getMessage()));
        }
    }
}
