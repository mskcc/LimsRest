package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.mskcc.domain.CorrectedCmoSampleView;

import java.util.List;

public interface SampleCounterRetriever {
    int retrieve(List<CorrectedCmoSampleView> patientCorrectedIds, String sampleClassAbbr);
}
