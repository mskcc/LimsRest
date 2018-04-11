package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.domain.sample.SampleType;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleTypeAbbreviationRetriever;
import org.mskcc.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class CspaceSampleTypeAbbreviationRetriever implements SampleTypeAbbreviationRetriever {
    private final static Log LOGGER = LogFactory.getLog(CspaceSampleTypeAbbreviationRetriever.class);

    private final static Map<NucleicAcid, String> nucleicAcid2Abbreviation = new HashMap<>();
    private final static Map<SampleType, String> sampleType2Abbreviation = new HashMap<>();

    static {
        nucleicAcid2Abbreviation.put(NucleicAcid.CFDNA, Constants.DNA_ABBREV);
        nucleicAcid2Abbreviation.put(NucleicAcid.DNA, Constants.DNA_ABBREV);
        nucleicAcid2Abbreviation.put(NucleicAcid.DNA_AND_RNA, Constants.DNA_ABBREV);
        nucleicAcid2Abbreviation.put(NucleicAcid.RNA, Constants.RNA_ABBREV);

        sampleType2Abbreviation.put(SampleType.DNA, Constants.DNA_ABBREV);
        sampleType2Abbreviation.put(SampleType.DNA_LIBRARY, Constants.DNA_ABBREV);
        sampleType2Abbreviation.put(SampleType.CFDNA, Constants.DNA_ABBREV);
        sampleType2Abbreviation.put(SampleType.RNA, Constants.RNA_ABBREV);
    }

    public static Map<NucleicAcid, String> getNucleicAcid2Abbreviation() {
        return new HashMap<>(nucleicAcid2Abbreviation);
    }

    private static String resolveByNucleicAcid(CorrectedCmoSampleView correctedCmoSampleView) {
        NucleicAcid nucleicAcid = correctedCmoSampleView.getNucleidAcid();

        LOGGER.info(String.format("Resolving Nucleic Acid Abbreviation with Nucleic Acid: \"%s\"", nucleicAcid));

        if (!nucleicAcid2Abbreviation.containsKey(nucleicAcid))
            throw new RuntimeException(String.format("No mapping for nucleic acid: %s", nucleicAcid));

        String nucleicAcidAbbrev = nucleicAcid2Abbreviation.get(nucleicAcid);
        LOGGER.info(String.format("Found mapping for Nucleic Acid %s => %s for sample", nucleicAcid,
                nucleicAcidAbbrev, correctedCmoSampleView.getId()));

        return nucleicAcidAbbrev;
    }

    @Override
    public String getNucleicAcidAbbr(CorrectedCmoSampleView correctedCmoSampleView) {
        SampleType sampleType = correctedCmoSampleView.getSampleType();

        if (sampleType2Abbreviation.containsKey(sampleType)) {
            LOGGER.info(String.format("Resolving Nucleic Acid Abbreviation with Sample Type: \"%s\"", sampleType));
            return sampleType2Abbreviation.get(sampleType);
        }

        return resolveByNucleicAcid(correctedCmoSampleView);
    }

    @Override
    public String getSampleTypeAbbr(CorrectedCmoSampleView correctedCmoSampleView) {
        SampleAbbreviationResolver resolver = SampleAbbreviationResolverFactory.getResolver(correctedCmoSampleView);
        return resolver.resolve(correctedCmoSampleView);
    }
}
