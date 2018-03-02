package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import org.mskcc.domain.sample.CorrectedCmoSampleView;

public interface SampleAbbreviationResolver {
    String resolve(CorrectedCmoSampleView correctedCmoSampleView);
}
