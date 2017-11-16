package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.CorrectedCmoIdConverterFactory;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.StringToSampleCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleId;
import org.mskcc.limsrest.staticstrings.Constants;

import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.stream.Collectors;

public class IncrementalSampleCounterRetriever implements SampleCounterRetriever {
    private final CorrectedCmoIdConverterFactory correctedCmoIdConverterFactory;

    public IncrementalSampleCounterRetriever(CorrectedCmoIdConverterFactory correctedCmoIdConverterFactory) {
        this.correctedCmoIdConverterFactory = correctedCmoIdConverterFactory;
    }

    @Override
    public int retrieve(List<CorrectedCmoSampleView> patientCorrectedViews, String sampleClassAbbr) {
        List<PatientCmoSampleId> cmoSampleIds = patientCorrectedViews.stream()
                .map(sampleCmoView -> {
                    StringToSampleCmoIdConverter converter = correctedCmoIdConverterFactory.getConverter
                            (sampleCmoView.getCorrectedCmoId());
                    return converter.convert(sampleCmoView);
                })
                .collect(Collectors.toList());

        OptionalLong maxSampleCount = cmoSampleIds.stream()
                .filter(s -> Objects.equals(s.getSampleTypeAbbr(), sampleClassAbbr))
                .mapToLong(PatientCmoSampleId::getSampleCount)
                .max();

        if (maxSampleCount.isPresent()) {
            long nextCounter = maxSampleCount.getAsLong() + 1;
            if (nextCounter >= Constants.SAMPLE_COUNT_MAX_VALUE)
                throw new SampleCounterOverflowException(String.format("Sample counter (%s) exceeds maximum supported" +
                        " value: %s", nextCounter, Constants.SAMPLE_COUNT_MAX_VALUE));

            return Math.toIntExact(nextCounter);
        }

        return 1;
    }

    public class SampleCounterOverflowException extends RuntimeException {
        public SampleCounterOverflowException(String message) {
            super(message);

        }
    }
}
