package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.mskcc.domain.sample.CorrectedCmoSampleView;

public interface SampleTypeAbbreviationRetriever {
    String getNucleicAcidAbbr(CorrectedCmoSampleView correctedCmoSampleView);

    String getSampleTypeAbbr(CorrectedCmoSampleView correctedCmoSampleView);
}
