package org.mskcc.limsrest.service.cmoinfo.retriever;

import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.service.cmoinfo.CmoSampleId;

import java.util.List;

public interface CmoSampleIdResolver<T extends CmoSampleId> {
    T resolve(CorrectedCmoSampleView correctedCmoSampleView, List<CorrectedCmoSampleView> patientSamples, String
            requestId);
}
