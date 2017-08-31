package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.mskcc.domain.BankedSample;
import org.mskcc.limsrest.limsapi.cmoinfo.CmoSampleId;

import java.util.List;

public interface CmoSampleIdResolver<T extends CmoSampleId> {
    T resolve(BankedSample bankedSample, List<String> patientCmoSampleIds);
}
