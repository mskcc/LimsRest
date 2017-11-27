package org.mskcc.limsrest.limsapi.dmp.converter;

import org.mskcc.domain.sample.BankedSample;
import org.mskcc.limsrest.limsapi.dmp.Study;

public interface DMPToBankedSampleConverter {
    BankedSample convert(Study study);
}
