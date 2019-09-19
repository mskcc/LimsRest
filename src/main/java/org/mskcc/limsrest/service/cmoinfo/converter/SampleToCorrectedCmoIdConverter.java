package org.mskcc.limsrest.service.cmoinfo.converter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.Recipe;
import org.mskcc.domain.sample.*;
import org.mskcc.limsrest.service.LimsException;
import org.mskcc.limsrest.service.PatientSamplesWithCmoInfoRetriever;
import org.mskcc.limsrest.util.Utils;

import java.util.function.Function;

public class SampleToCorrectedCmoIdConverter implements CorrectedCmoIdConverter<Sample> {
    private final static Log LOGGER = LogFactory.getLog(PatientSamplesWithCmoInfoRetriever.class);

    @Override
    public CorrectedCmoSampleView convert(Sample sample) throws LimsException {
        LOGGER.debug(String.format("Converting sample %s to Corrected Cmo Sample View", sample));

        try {
            validate(sample);
            CorrectedCmoSampleView correctedCmoSampleView = new CorrectedCmoSampleView(sample.getIgoId());
            correctedCmoSampleView.setCorrectedCmoId(sample.getCorrectedCmoSampleId());
            correctedCmoSampleView.setSampleType(SampleType.fromString(sample.getExemplarSampleType()));

            correctedCmoSampleView.setSampleId(sample.getCmoSampleInfo().getUserSampleID());

            Utils.getOptionalNucleicAcid(sample.getNAtoExtract(), sample.getIgoId()).ifPresent
                    (correctedCmoSampleView::setNucleidAcid);

            correctedCmoSampleView.setPatientId(sample.getCmoSampleInfo().getCmoPatientId());
            correctedCmoSampleView.setNormalizedPatientId(sample.getCmoSampleInfo().getNormalizedPatientId());
            correctedCmoSampleView.setRequestId(sample.getRequestId());

            if (!StringUtils.isEmpty(sample.get(Sample.RECIPE)))
                correctedCmoSampleView.setRecipe(Recipe.getRecipeByValue(sample.get(Sample.RECIPE)));

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
            throw new RuntimeException(String.format("Error for sample %s: %s",
                    sample.getIgoId(), e.getMessage()), e);
        }
    }

    private void validate(Sample sample) {
        StringBuilder error = new StringBuilder();

        error = appendToError(sample, s -> SampleType.fromString(sample.getExemplarSampleType()), error);

        if (error.length() > 0)
            throw new RuntimeException(error.toString());
    }

    private StringBuilder appendToError(Sample sample, Function<Sample, Object> function, StringBuilder error) {
        try {
            function.apply(sample);
        } catch (Exception e) {
            error.append(e.getMessage()).append(", ");
        }

        return error;
    }
}
