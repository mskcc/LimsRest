package org.mskcc.limsrest.limsapi.cmoinfo.patientsample;

import com.google.common.base.Preconditions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.CmoSampleIdResolver;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleCounterRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleTypeAbbreviationRetriever;
import org.mskcc.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class PatientCmoSampleIdResolver implements CmoSampleIdResolver<PatientAwareCmoSampleId> {
    private static final Log LOG = LogFactory.getLog(PatientCmoSampleIdResolver.class);

    private final SampleCounterRetriever sampleCounterRetriever;
    private final SampleTypeAbbreviationRetriever sampleTypeAbbreviationRetriever;

    @Autowired
    public PatientCmoSampleIdResolver(SampleCounterRetriever sampleCounterRetriever, SampleTypeAbbreviationRetriever
            sampleTypeAbbreviationRetriever) {
        this.sampleCounterRetriever = sampleCounterRetriever;
        this.sampleTypeAbbreviationRetriever = sampleTypeAbbreviationRetriever;
    }

    @Override
    public PatientAwareCmoSampleId resolve(CorrectedCmoSampleView correctedCmoSampleView, List<CorrectedCmoSampleView>
            cmoSampleViews, String requestId) {
        validateRequiredFields(correctedCmoSampleView);

        LOG.debug(String.format("Resolving corrected cmo id for patient correctedCmoSampleId: %s",
                correctedCmoSampleView.getId()));

        String sampleClassAbbr = sampleTypeAbbreviationRetriever.getSampleTypeAbbr(correctedCmoSampleView);
        int sampleCount = sampleCounterRetriever.retrieve(correctedCmoSampleView, cmoSampleViews, sampleClassAbbr);

        return new PatientAwareCmoSampleId(
                correctedCmoSampleView.getPatientId(),
                sampleClassAbbr,
                sampleCount,
                sampleTypeAbbreviationRetriever.getNucleicAcidAbbr(correctedCmoSampleView));
    }

    private void validateRequiredFields(CorrectedCmoSampleView correctedCmoSampleView) {
        CommonUtils.requireNonNullNorEmpty(correctedCmoSampleView.getPatientId(), String.format("Patient id is not " +
                "set for banked sample: %s", correctedCmoSampleView.getId()));
        Preconditions.checkNotNull(correctedCmoSampleView.getSpecimenType(), String.format("Specimen type is not set " +
                "for banked sample: %s", correctedCmoSampleView.getId()));
        Preconditions.checkNotNull(correctedCmoSampleView.getNucleidAcid(), String.format("Nucleic acid is not set " +
                "for banked sample: %s", correctedCmoSampleView.getId()));
    }
}
