package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.mskcc.domain.BankedSample;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.domain.sample.SampleClass;
import org.mskcc.domain.sample.SampleOrigin;
import org.mskcc.domain.sample.SpecimenType;
import org.mskcc.limsrest.limsapi.cmoinfo.PatientCmoSampleId;
import org.mskcc.util.CommonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatientCmoSampleIdResolver implements CmoSampleIdResolver<PatientCmoSampleId> {
    private final static Map<SampleClass, String> sampleClassToAbbreviation = new HashMap<>();
    private final static Map<SampleOrigin, String> sampleOriginToAbbreviation = new HashMap<>();
    private final static Map<SpecimenType, String> specimenTypeToAbbreviation = new HashMap<>();
    private final static Map<NucleicAcid, String> nucleicAcidToAbbreviation = new HashMap<>();

    static {
        initAbbreviationMappings();
    }

    private PatientSampleCountRetriever patientSampleCountRetriever;

    public PatientCmoSampleIdResolver(PatientSampleCountRetriever patientSampleCountRetriever) {
        this.patientSampleCountRetriever = patientSampleCountRetriever;
    }

    private static void initAbbreviationMappings() {
        sampleClassToAbbreviation.put(SampleClass.ADJACENT_NORMAL, "N");
        sampleClassToAbbreviation.put(SampleClass.ADJACENT_TISSUE, "T");
        sampleClassToAbbreviation.put(SampleClass.LOCAL_RECURRENCE, "R");
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

    private static String getNucleicAcidAbbr(BankedSample bankedSample) {
        return nucleicAcidToAbbreviation.get(NucleicAcid.fromValue(bankedSample.getNucleicAcidType()));
    }

    public static Map<SampleClass, String> getSampleClassToAbbreviation() {
        return sampleClassToAbbreviation;
    }

    private String getSampleClassAbbr(BankedSample bankedSample) {
        if (shouldResolveBySpecimenType(bankedSample))
            return resolveBySpecimenType(bankedSample);
        if (isCfDna(bankedSample))
            return resolveBySampleOrigin(bankedSample);
        return resolveBySampleClass(bankedSample);
    }

    private String resolveBySpecimenType(BankedSample bankedSample) {
        return specimenTypeToAbbreviation.get(SpecimenType.fromValue(bankedSample.getSpecimenType()));
    }

    private String resolveBySampleClass(BankedSample bankedSample) {
        CommonUtils.requireNonNullNorEmpty(bankedSample.getSampleClass(), String.format("Sample class is not set for banked sample: %s", bankedSample.getUserSampleId()));

        if (isCellFree(bankedSample))
            return resolveBySampleOrigin(bankedSample);

        return sampleClassToAbbreviation.get(SampleClass.fromValue(bankedSample.getSampleClass()));
    }

    private boolean isCellFree(BankedSample bankedSample) {
        return SampleClass.fromValue(bankedSample.getSampleClass()) == SampleClass.CELL_FREE;
    }

    private String resolveBySampleOrigin(BankedSample bankedSample) {
        CommonUtils.requireNonNullNorEmpty(bankedSample.getSampleOrigin(), String.format("Sample origin is not set for Cell free banked sample: %s", bankedSample.getOtherSampleId()));
        return sampleOriginToAbbreviation.get(SampleOrigin.fromValue(bankedSample.getSampleOrigin()));
    }

    private boolean shouldResolveBySpecimenType(BankedSample bankedSample) {
        return isXenograft(bankedSample) || isOgranoid(bankedSample);
    }

    private boolean isOgranoid(BankedSample bankedSample) {
        return SpecimenType.fromValue(bankedSample.getSpecimenType()) == SpecimenType.ORGANOID;
    }

    private boolean isXenograft(BankedSample bankedSample) {
        return SpecimenType.fromValue(bankedSample.getSpecimenType()) == SpecimenType.PDX || SpecimenType.fromValue(bankedSample.getSpecimenType()) == SpecimenType.XENOGRAFTDERIVEDCELLLINE || SpecimenType.fromValue(bankedSample.getSpecimenType()) == SpecimenType.XENOGRAFT;
    }

    private boolean isCfDna(BankedSample bankedSample) {
        return SpecimenType.fromValue(bankedSample.getSpecimenType()) == SpecimenType.CFDNA;
    }

    @Override
    public PatientCmoSampleId resolve(BankedSample bankedSample, List<String> patientCmoSampleIds) {
        validateRequiredFields(bankedSample);
        String sampleClassAbbr = getSampleClassAbbr(bankedSample);
        long sampleCount = patientSampleCountRetriever.retrieve(patientCmoSampleIds, sampleClassAbbr);

        return new PatientCmoSampleId(
                bankedSample.getPatientId(),
                sampleClassAbbr,
                sampleCount,
                getNucleicAcidAbbr(bankedSample));
    }

    private void validateRequiredFields(BankedSample bankedSample) {
        CommonUtils.requireNonNullNorEmpty(bankedSample.getPatientId(), String.format("Patient id is not set for banked sample: %s", bankedSample.getUserSampleId()));
        CommonUtils.requireNonNullNorEmpty(bankedSample.getSpecimenType(), String.format("Specimen type is not set for banked sample: %s", bankedSample.getUserSampleId()));
        CommonUtils.requireNonNullNorEmpty(bankedSample.getNucleicAcidType(), String.format("Nucleic acid is not set for banked sample: %s", bankedSample.getUserSampleId()));
    }
}
