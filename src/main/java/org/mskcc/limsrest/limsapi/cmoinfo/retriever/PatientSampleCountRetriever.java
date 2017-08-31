package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.mskcc.limsrest.limsapi.cmoinfo.PatientCmoSampleId;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.StringToSampleCmoIdConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

public class PatientSampleCountRetriever {
    private final StringToSampleCmoIdConverter sampleCmoIdStringToCmoIdConverter;

    public PatientSampleCountRetriever(StringToSampleCmoIdConverter sampleCmoIdStringToCmoIdConverter) {
        this.sampleCmoIdStringToCmoIdConverter = sampleCmoIdStringToCmoIdConverter;
    }

    public long retrieve(List<String> sampleCmoIdStrings, String sampleClassAbbr) {
        List<PatientCmoSampleId> cmoSampleIds = new ArrayList<>();
        for (String sampleCmoId : sampleCmoIdStrings) {
            PatientCmoSampleId cmoSampleId = sampleCmoIdStringToCmoIdConverter.convert(sampleCmoId);
            cmoSampleIds.add(cmoSampleId);
        }

        OptionalLong maxSampleCount = cmoSampleIds.stream()
                .filter(s -> s.getSampleTypeAbbr().equals(sampleClassAbbr))
                .mapToLong(s -> s.getSampleCount())
                .max();

        if (maxSampleCount.isPresent())
            return maxSampleCount.getAsLong() + 1;

        return 1l;
    }
}
