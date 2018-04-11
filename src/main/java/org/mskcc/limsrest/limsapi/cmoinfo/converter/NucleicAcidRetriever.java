package org.mskcc.limsrest.limsapi.cmoinfo.converter;

import org.mskcc.domain.sample.BankedSample;
import org.mskcc.domain.sample.NucleicAcid;

public interface NucleicAcidRetriever {
    NucleicAcid retrieve(BankedSample bankedSample);
}
