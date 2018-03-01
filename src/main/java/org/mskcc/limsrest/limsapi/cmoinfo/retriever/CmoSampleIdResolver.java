package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.CmoSampleId;

import java.util.List;

public interface CmoSampleIdResolver<T extends CmoSampleId> {
    T resolve(CorrectedCmoSampleView correctedCmoSampleView, List<CorrectedCmoSampleView> patientSamples, String
            requestId);
}
