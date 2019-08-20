package org.mskcc.limsrest.service.cmoinfo.cspace;

import org.mskcc.domain.sample.CorrectedCmoSampleView;

public interface SampleAbbreviationResolver {
    String resolve(CorrectedCmoSampleView correctedCmoSampleView);
}
