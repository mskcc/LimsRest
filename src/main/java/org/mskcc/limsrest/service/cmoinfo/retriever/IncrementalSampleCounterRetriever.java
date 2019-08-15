package org.mskcc.limsrest.service.cmoinfo.retriever;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.service.cmoinfo.converter.CorrectedCmoIdConverterFactory;
import org.mskcc.limsrest.service.cmoinfo.converter.CorrectedCmoSampleViewToSampleCmoIdConverter;
import org.mskcc.limsrest.service.cmoinfo.patientsample.PatientAwareCmoSampleId;
import org.mskcc.limsrest.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.stream.Collectors;

public class IncrementalSampleCounterRetriever implements SampleCounterRetriever {
    public static final int DEFAULT_COUNTER = 1;
    private final static Log LOGGER = LogFactory.getLog(IncrementalSampleCounterRetriever.class);
    private final CorrectedCmoIdConverterFactory correctedCmoIdConverterFactory;

    public IncrementalSampleCounterRetriever(CorrectedCmoIdConverterFactory correctedCmoIdConverterFactory) {
        this.correctedCmoIdConverterFactory = correctedCmoIdConverterFactory;
    }

    @Override
    public int retrieve(CorrectedCmoSampleView correctedCmoSampleView, List<CorrectedCmoSampleView>
            patientCorrectedViews, String sampleClassAbbr) {
        if (isCounterSet(correctedCmoSampleView)) {
            LOGGER.info(String.format("Cmo Sample id counter is set to value: %d. This value will be used.",
                    correctedCmoSampleView.getCounter()));
            return correctedCmoSampleView.getCounter();
        }

        List<PatientAwareCmoSampleId> cmoSampleIds = new ArrayList<>();

        LOGGER.info(String.format("Resolving sample counter out of patient samples: %s", patientCorrectedViews));

        for (CorrectedCmoSampleView sampleCmoView : patientCorrectedViews) {
            try {
                CorrectedCmoSampleViewToSampleCmoIdConverter converter = correctedCmoIdConverterFactory.getConverter
                        (sampleCmoView.getCorrectedCmoId());

                PatientAwareCmoSampleId patientAwareCmoSampleId = converter.convert(sampleCmoView);
                cmoSampleIds.add(patientAwareCmoSampleId);
            } catch (Exception e) {
                LOGGER.warn(String.format("Error while retrieving information about current patient's sample: %s. " +
                        "This sample won't be counted to retrieve sample counter", sampleCmoView.getId()), e);
            }
        }

        List<PatientAwareCmoSampleId> samplesWithSameAbbr = cmoSampleIds.stream()
                .filter(s -> Objects.equals(s.getSampleTypeAbbr(), sampleClassAbbr))
                .collect(Collectors.toList());

        LOGGER.info(String.format("Found %d samples with current sample's abbreviation \"%s\": %s",
                samplesWithSameAbbr.size(), sampleClassAbbr, samplesWithSameAbbr));

        OptionalLong maxSampleCount = samplesWithSameAbbr.stream()
                .mapToLong(PatientAwareCmoSampleId::getSampleCount)
                .max();

        if (maxSampleCount.isPresent()) {
            LOGGER.info(String.format("Max current counter found: %d", maxSampleCount.getAsLong()));
            long nextCounter = maxSampleCount.getAsLong() + 1;

            LOGGER.info(String.format("Next counter to be set: %d", nextCounter));

            if (nextCounter >= Constants.SAMPLE_COUNT_MAX_VALUE)
                throw new SampleCounterOverflowException(String.format("Sample counter (%s) exceeds maximum supported" +
                        " value: %s", nextCounter, Constants.SAMPLE_COUNT_MAX_VALUE));

            return Math.toIntExact(nextCounter);
        }

        LOGGER.info(String.format("No other samples found for patient %s with sample abbreviation %s. Setting default" +
                " counter value: %d", getPatientId(patientCorrectedViews), sampleClassAbbr, DEFAULT_COUNTER));

        return DEFAULT_COUNTER;
    }

    private boolean isCounterSet(CorrectedCmoSampleView correctedCmoSampleView) {
        return correctedCmoSampleView.getCounter() != null;
    }

    private String getPatientId(List<CorrectedCmoSampleView> patientCorrectedViews) {
        if (patientCorrectedViews.size() == 0)
            return "";
        return patientCorrectedViews.get(0).getPatientId();
    }

    public class SampleCounterOverflowException extends RuntimeException {
        public SampleCounterOverflowException(String message) {
            super(message);
        }
    }
}
