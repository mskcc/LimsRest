package org.mskcc.limsrest.service.cmoinfo.cspace;

import com.google.common.base.Preconditions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.domain.sample.SampleClass;

import java.util.HashMap;
import java.util.Map;

public class ClassSampleAbbreviationResolver implements SampleAbbreviationResolver {
    private final static Log LOGGER = LogFactory.getLog(ClassSampleAbbreviationResolver.class);

    private final static Map<SampleClass, String> sampleClassToAbbreviation = new HashMap<>();

    static {
        sampleClassToAbbreviation.put(SampleClass.ADJACENT_NORMAL, "N");
        sampleClassToAbbreviation.put(SampleClass.ADJACENT_TISSUE, "T");
        sampleClassToAbbreviation.put(SampleClass.LOCAL_RECURRENCE, "R");
        sampleClassToAbbreviation.put(SampleClass.RECURRENCE, "R");
        sampleClassToAbbreviation.put(SampleClass.METASTASIS, "M");
        sampleClassToAbbreviation.put(SampleClass.NORMAL, "N");
        sampleClassToAbbreviation.put(SampleClass.PRIMARY, "P");
        sampleClassToAbbreviation.put(SampleClass.TUMOR, "T");
        sampleClassToAbbreviation.put(SampleClass.UNKNOWN_TUMOR, "T");
    }

    public static Map<SampleClass, String> getSampleClassToAbbreviation() {
        return sampleClassToAbbreviation;
    }

    @Override
    public String resolve(CorrectedCmoSampleView correctedCmoSampleView) {
        Preconditions.checkNotNull(correctedCmoSampleView.getSampleClass(), String.format("Sample class is not set " +
                "for sample: %s", correctedCmoSampleView.getId()));

        LOGGER.info(String.format("Resolving Sample Type Abbreviation for sample: %s by Sample Class",
                correctedCmoSampleView.getId()));

        if (!sampleClassToAbbreviation.containsKey(correctedCmoSampleView.getSampleClass())) {
            if (correctedCmoSampleView.getSampleClass() == SampleClass.CELL_FREE)
                throw new CellFreeSampleNotSupportedException();
            throw new RuntimeException(String.format("No mapping for sample class: %s", correctedCmoSampleView
                    .getSampleClass()));
        }

        String sampleTypeAbbrev = sampleClassToAbbreviation.get(correctedCmoSampleView.getSampleClass());

        LOGGER.info(String.format("Found mapping for Sample Class %s => %s for sample: %s", correctedCmoSampleView
                .getSampleClass(), sampleTypeAbbrev, correctedCmoSampleView.getId()));

        return sampleTypeAbbrev;
    }

    static class CellFreeSampleNotSupportedException extends RuntimeException {
    }
}
