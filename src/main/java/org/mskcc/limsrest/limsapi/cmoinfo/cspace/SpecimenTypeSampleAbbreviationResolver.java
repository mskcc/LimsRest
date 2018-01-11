package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.domain.sample.SpecimenType;

import java.util.HashMap;
import java.util.Map;

public class SpecimenTypeSampleAbbreviationResolver implements SampleAbbreviationResolver {
    private final static Log LOGGER = LogFactory.getLog(SpecimenTypeSampleAbbreviationResolver.class);
    private final static Map<SpecimenType, String> specimenTypeToAbbreviation = new HashMap<>();

    static {
        specimenTypeToAbbreviation.put(SpecimenType.PDX, "X");
        specimenTypeToAbbreviation.put(SpecimenType.XENOGRAFT, "X");
        specimenTypeToAbbreviation.put(SpecimenType.XENOGRAFTDERIVEDCELLLINE, "X");
        specimenTypeToAbbreviation.put(SpecimenType.ORGANOID, "G");
    }

    public static Map<SpecimenType, String> getSpecimenTypeToAbbreviation() {
        return specimenTypeToAbbreviation;
    }

    @Override
    public String resolve(CorrectedCmoSampleView correctedCmoSampleView) {
        if (!specimenTypeToAbbreviation.containsKey(correctedCmoSampleView.getSpecimenType()))
            throw new RuntimeException(String.format("No mapping for specimen type: %s", correctedCmoSampleView
                    .getSpecimenType()));

        String sampleTypeAbbrev = specimenTypeToAbbreviation.get(correctedCmoSampleView.getSpecimenType());

        LOGGER.info(String.format("Found mapping for Specimen Type %s => %s for sample: %s", correctedCmoSampleView
                .getSpecimenType(), sampleTypeAbbrev, correctedCmoSampleView.getId()));

        return sampleTypeAbbrev;
    }

}
