package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.mskcc.domain.CorrectedCmoSampleView;

public interface SampleAbbreviationRetriever {
    String retrieve(CorrectedCmoSampleView correctedCmoSampleView);
}
