package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.domain.sample.SpecimenType;

public class SampleAbbreviationResolverFactory {
    private final static Log LOGGER = LogFactory.getLog(SampleAbbreviationResolverFactory.class);

    public static SampleAbbreviationResolver getResolver(CorrectedCmoSampleView correctedCmoSampleView) {
        if (shouldResolveBySpecimenType(correctedCmoSampleView))
            return new SpecimenTypeSampleAbbreviationResolver();
        if (isCfDna(correctedCmoSampleView))
            return new CfDnaSampleAbbreviationResolver();
        if (isExosome(correctedCmoSampleView))
            return new ExosomeSampleAbbreviationResolver();
        return new ClassSampleAbbreviationResolver();
    }

    private static boolean isExosome(CorrectedCmoSampleView correctedCmoSampleView) {
        return correctedCmoSampleView.getSpecimenType() == SpecimenType.EXOSOME;
    }

    private static boolean shouldResolveBySpecimenType(CorrectedCmoSampleView correctedCmoSampleView) {
        boolean shouldResolveBySpecimenType = isXenograft(correctedCmoSampleView) || isOgranoid(correctedCmoSampleView);

        if (shouldResolveBySpecimenType)
            LOGGER.info(String.format("Resolving Sample Type abbreviation for sample: %s by Specimen Type",
                    correctedCmoSampleView.getId()));

        return shouldResolveBySpecimenType;
    }


    private static boolean isOgranoid(CorrectedCmoSampleView correctedCmoSampleView) {
        return correctedCmoSampleView.getSpecimenType() == SpecimenType.ORGANOID;
    }

    private static boolean isXenograft(CorrectedCmoSampleView correctedCmoSampleView) {
        return correctedCmoSampleView.getSpecimenType() == SpecimenType.PDX || correctedCmoSampleView.getSpecimenType
                () == SpecimenType.XENOGRAFTDERIVEDCELLLINE || correctedCmoSampleView.getSpecimenType() ==
                SpecimenType.XENOGRAFT;
    }

    private static boolean isCfDna(CorrectedCmoSampleView correctedCmoSampleView) {
        boolean shouldResolveBySampleOrigin = isCfDNA(correctedCmoSampleView);

        if (shouldResolveBySampleOrigin)
            LOGGER.info(String.format("Sample: %s has Specimen Type thus Sample Type Abbreviation will be resolved by" +
                    " Sample Origin", correctedCmoSampleView.getId()));

        return shouldResolveBySampleOrigin;
    }

    private static boolean isCfDNA(CorrectedCmoSampleView correctedCmoSampleView) {
        return correctedCmoSampleView.getSpecimenType() == SpecimenType.CFDNA;
    }
}
