package org.mskcc.limsrest.service.cmoinfo.cspace;

import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.domain.sample.SampleOrigin;

import java.util.HashMap;
import java.util.Map;

public class ExosomeSampleAbbreviationResolver implements SampleAbbreviationResolver {
    private final static Map<SampleOrigin, String> exosomeSampleOriginToAbbreviation = new HashMap<>();
    private final static String DEFAULT_ABBREVIATION = "T";

    static {
        exosomeSampleOriginToAbbreviation.put(SampleOrigin.PLASMA, "L");
        exosomeSampleOriginToAbbreviation.put(SampleOrigin.WHOLE_BLOOD, "L");
    }

    @Override
    public String resolve(CorrectedCmoSampleView correctedCmoSampleView) {
        return exosomeSampleOriginToAbbreviation.getOrDefault(correctedCmoSampleView.getSampleOrigin(),
                DEFAULT_ABBREVIATION);
    }
}
