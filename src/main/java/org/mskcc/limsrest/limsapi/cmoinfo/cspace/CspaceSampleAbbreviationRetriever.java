package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.domain.sample.SampleClass;
import org.mskcc.domain.sample.SampleOrigin;
import org.mskcc.domain.sample.SpecimenType;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleAbbreviationRetriever;

import java.util.HashMap;
import java.util.Map;

public class CspaceSampleAbbreviationRetriever implements SampleAbbreviationRetriever {
    private static final Logger LOGGER = Logger.getLogger(CspaceSampleAbbreviationRetriever.class);

    private final static Map<SampleClass, String> sampleClassToAbbreviation = new HashMap<>();
    private final static Map<SampleOrigin, String> sampleOriginToAbbreviation = new HashMap<>();
    private final static Map<SpecimenType, String> specimenTypeToAbbreviation = new HashMap<>();
    private final static Map<NucleicAcid, String> nucleicAcidToAbbreviation = new HashMap<>();

    static {
        sampleClassToAbbreviation.put(SampleClass.ADJACENT_NORMAL, "N");
        sampleClassToAbbreviation.put(SampleClass.ADJACENT_TISSUE, "T");
        sampleClassToAbbreviation.put(SampleClass.LOCAL_RECURRENCE, "R");
        sampleClassToAbbreviation.put(SampleClass.RECURRENCE, "R");
        sampleClassToAbbreviation.put(SampleClass.METASTASIS, "M");
        sampleClassToAbbreviation.put(SampleClass.NORMAL, "N");
        sampleClassToAbbreviation.put(SampleClass.PRIMARY, "P");
        sampleClassToAbbreviation.put(SampleClass.UNKNOWN_TUMOR, "T");

        nucleicAcidToAbbreviation.put(NucleicAcid.DNA, "d");
        nucleicAcidToAbbreviation.put(NucleicAcid.RNA, "r");

        sampleOriginToAbbreviation.put(SampleOrigin.URINE, "U");
        sampleOriginToAbbreviation.put(SampleOrigin.CEREBROSPINAL_FLUID, "S");
        sampleOriginToAbbreviation.put(SampleOrigin.PLASMA, "L");
        sampleOriginToAbbreviation.put(SampleOrigin.WHOLE_BLOOD, "L");

        specimenTypeToAbbreviation.put(SpecimenType.PDX, "X");
        specimenTypeToAbbreviation.put(SpecimenType.XENOGRAFT, "X");
        specimenTypeToAbbreviation.put(SpecimenType.XENOGRAFTDERIVEDCELLLINE, "X");
        specimenTypeToAbbreviation.put(SpecimenType.ORGANOID, "G");
    }

    public static Map<SampleOrigin, String> getSampleOriginToAbbreviation() {
        return new HashMap<>(sampleOriginToAbbreviation);
    }

    public static Map<NucleicAcid, String> getNucleicAcidToAbbreviation() {
        return new HashMap<>(nucleicAcidToAbbreviation);
    }

    public static Map<SpecimenType, String> getSpecimenTypeToAbbreviation() {
        return specimenTypeToAbbreviation;
    }

    public static Map<SampleClass, String> getSampleClassToAbbreviation() {
        return sampleClassToAbbreviation;
    }

    public static String getNucleicAcidAbbr(CorrectedCmoSampleView correctedCmoSampleView) {
        NucleicAcid nucleicAcid = correctedCmoSampleView.getNucleidAcid();

        if (!nucleicAcidToAbbreviation.containsKey(nucleicAcid))
            throw new RuntimeException(String.format("No mapping for nucleid acid: %s", nucleicAcid));

        String nucleidAcidAbbrev = nucleicAcidToAbbreviation.get(nucleicAcid);
        LOGGER.info(String.format("Found mapping for Nucleid Acid %s => %s for sample", nucleicAcid,
                nucleidAcidAbbrev, correctedCmoSampleView.getId()));

        return nucleidAcidAbbrev;
    }

    @Override
    public String retrieve(CorrectedCmoSampleView correctedCmoSampleView) {
        if (shouldResolveBySpecimenType(correctedCmoSampleView))
            return resolveBySpecimenType(correctedCmoSampleView);
        if (shouldResolveBySampleOrigin(correctedCmoSampleView))
            return resolveBySampleOrigin(correctedCmoSampleView);
        return resolveBySampleClass(correctedCmoSampleView);
    }

    private String resolveBySpecimenType(CorrectedCmoSampleView correctedCmoSampleView) {
        if (!specimenTypeToAbbreviation.containsKey(correctedCmoSampleView.getSpecimenType()))
            throw new RuntimeException(String.format("No mapping for specimen type: %s", correctedCmoSampleView
                    .getSpecimenType()));

        String sampleTypeAbbrev = specimenTypeToAbbreviation.get(correctedCmoSampleView.getSpecimenType());

        LOGGER.info(String.format("Found mapping for Specimen Type %s => %s for sample: %s", correctedCmoSampleView
                .getSpecimenType(), sampleTypeAbbrev, correctedCmoSampleView.getId()));

        return sampleTypeAbbrev;
    }

    private String resolveBySampleClass(CorrectedCmoSampleView correctedCmoSampleView) {
        Preconditions.checkNotNull(correctedCmoSampleView.getSampleClass(), String.format("Sample class is not set " +
                "for sample: %s", correctedCmoSampleView.getId()));

        LOGGER.info(String.format("Resolving Sample Type Abbreviation for sample: %s by Sample Class",
                correctedCmoSampleView.getId()));

        if (!sampleClassToAbbreviation.containsKey(correctedCmoSampleView.getSampleClass()))
            throw new RuntimeException(String.format("No mapping for sample class: %s", correctedCmoSampleView
                    .getSampleClass()));

        String sampleTypeAbbrev = sampleClassToAbbreviation.get(correctedCmoSampleView.getSampleClass());

        LOGGER.info(String.format("Found mapping for Sample Class %s => %s for sample: %s", correctedCmoSampleView
                .getSampleClass(), sampleTypeAbbrev, correctedCmoSampleView.getId()));

        return sampleTypeAbbrev;
    }

    private String resolveBySampleOrigin(CorrectedCmoSampleView correctedCmoSampleView) {
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

    private boolean shouldResolveBySpecimenType(CorrectedCmoSampleView correctedCmoSampleView) {
        boolean shouldResolveBySpecimenType = isXenograft(correctedCmoSampleView) || isOgranoid(correctedCmoSampleView);

        if (shouldResolveBySpecimenType)
            LOGGER.info(String.format("Resolving Sample Type abbreviation for sample: %s by Specimen Type",
                    correctedCmoSampleView.getId()));

        return shouldResolveBySpecimenType;
    }

    private boolean isXenograft(CorrectedCmoSampleView correctedCmoSampleView) {
        return correctedCmoSampleView.getSpecimenType() == SpecimenType.PDX || correctedCmoSampleView.getSpecimenType
                () == SpecimenType.XENOGRAFTDERIVEDCELLLINE || correctedCmoSampleView.getSpecimenType() ==
                SpecimenType.XENOGRAFT;
    }

    private boolean isOgranoid(CorrectedCmoSampleView correctedCmoSampleView) {
        return correctedCmoSampleView.getSpecimenType() == SpecimenType.ORGANOID;
    }

    private boolean shouldResolveBySampleOrigin(CorrectedCmoSampleView correctedCmoSampleView) {
        boolean shouldResolveBySampleOrigin = isCfDNA(correctedCmoSampleView) || isCellFree(correctedCmoSampleView);

        if (shouldResolveBySampleOrigin)
            LOGGER.info(String.format("Sample: %s has Specimen Type thus Sample Type Abbreviation will be resolved by" +
                    " Sample Origin", correctedCmoSampleView.getId()));

        return shouldResolveBySampleOrigin;
    }

    private boolean isCfDNA(CorrectedCmoSampleView correctedCmoSampleView) {
        return correctedCmoSampleView.getSpecimenType() == SpecimenType.CFDNA;
    }

    private boolean isCellFree(CorrectedCmoSampleView correctedCmoSampleView) {
        return correctedCmoSampleView.getSampleClass() == SampleClass.CELL_FREE;
    }
}
