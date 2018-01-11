package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import org.mskcc.domain.CorrectedCmoSampleView;

public interface SampleAbbreviationResolver {
    String resolve(CorrectedCmoSampleView correctedCmoSampleView);
}
