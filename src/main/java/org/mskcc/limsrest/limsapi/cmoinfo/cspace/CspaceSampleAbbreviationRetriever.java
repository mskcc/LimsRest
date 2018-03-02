package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleAbbreviationRetriever;

import java.util.HashMap;
import java.util.Map;

public class CspaceSampleAbbreviationRetriever implements SampleAbbreviationRetriever {
    private final static Log LOGGER = LogFactory.getLog(CspaceSampleAbbreviationRetriever.class);

    private final static Map<NucleicAcid, String> nucleicAcidToAbbreviation = new HashMap<>();

    static {
        nucleicAcidToAbbreviation.put(NucleicAcid.DNA, "d");
        nucleicAcidToAbbreviation.put(NucleicAcid.RNA, "r");
    }

    public static Map<NucleicAcid, String> getNucleicAcidToAbbreviation() {
        return new HashMap<>(nucleicAcidToAbbreviation);
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
        SampleAbbreviationResolver resolver = SampleAbbreviationResolverFactory.getResolver(correctedCmoSampleView);
        return resolver.resolve(correctedCmoSampleView);
    }
}
