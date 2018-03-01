package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import com.google.common.base.Preconditions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.domain.sample.SampleOrigin;

import java.util.HashMap;
import java.util.Map;

public class CfDnaSampleAbbreviationResolver implements SampleAbbreviationResolver {
    private final static Log LOGGER = LogFactory.getLog(CfDnaSampleAbbreviationResolver.class);

    private final static Map<SampleOrigin, String> sampleOriginToAbbreviation = new HashMap<>();

    static {
        sampleOriginToAbbreviation.put(SampleOrigin.URINE, "U");
        sampleOriginToAbbreviation.put(SampleOrigin.CEREBROSPINAL_FLUID, "S");
        sampleOriginToAbbreviation.put(SampleOrigin.PLASMA, "L");
        sampleOriginToAbbreviation.put(SampleOrigin.WHOLE_BLOOD, "L");
    }

    public static Map<SampleOrigin, String> getSampleOriginToAbbreviation() {
        return new HashMap<>(sampleOriginToAbbreviation);
    }

    @Override
    public String resolve(CorrectedCmoSampleView correctedCmoSampleView) {
        Preconditions.checkNotNull(correctedCmoSampleView.getSampleOrigin(), String.format("Sample origin is not set " +
                "for Cell free sample: %s", correctedCmoSampleView.getId()));

        if (!sampleOriginToAbbreviation.containsKey(correctedCmoSampleView.getSampleOrigin()))
            throw new RuntimeException(String.format("No mapping for sample origin: %s", correctedCmoSampleView
                    .getSampleOrigin()));


        String sampleTypeAbbrev = sampleOriginToAbbreviation.get(correctedCmoSampleView.getSampleOrigin());

        LOGGER.info(String.format("Found mapping for Sample Origin %s => %s for sample: %s", correctedCmoSampleView
                .getSpecimenType(), sampleTypeAbbrev, correctedCmoSampleView.getId()));

        return sampleTypeAbbrev;
    }
}
