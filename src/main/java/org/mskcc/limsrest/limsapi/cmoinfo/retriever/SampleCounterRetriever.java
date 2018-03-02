package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import org.mskcc.domain.sample.CorrectedCmoSampleView;

import java.util.List;

public interface SampleCounterRetriever {
    int retrieve(CorrectedCmoSampleView correctedCmoSampleView, List<CorrectedCmoSampleView> patientCorrectedIds, String sampleClassAbbr);
}
