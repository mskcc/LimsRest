package org.mskcc.limsrest.limsapi.cmoinfo.patientsample;

import com.google.common.base.Preconditions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.cspace.CspaceSampleAbbreviationRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.CmoSampleIdResolver;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleAbbreviationRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleCounterRetriever;
import org.mskcc.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class PatientCmoSampleIdResolver implements CmoSampleIdResolver<PatientCmoSampleId> {
    private static final Log LOG = LogFactory.getLog(PatientCmoSampleIdResolver.class);

    private final SampleCounterRetriever sampleCounterRetriever;
    private final SampleAbbreviationRetriever sampleTypeAbbreviationRetriever;

    @Autowired
    public PatientCmoSampleIdResolver(SampleCounterRetriever sampleCounterRetriever, SampleAbbreviationRetriever
            sampleTypeAbbreviationRetriever) {
        this.sampleCounterRetriever = sampleCounterRetriever;
        this.sampleTypeAbbreviationRetriever = sampleTypeAbbreviationRetriever;
    }

    @Override
    public PatientCmoSampleId resolve(CorrectedCmoSampleView correctedCmoSampleView, List<CorrectedCmoSampleView>
            cmoSampleViews, String requestId) {
        validateRequiredFields(correctedCmoSampleView);

        LOG.debug(String.format("Resolving corrected cmo id for patient correctedCmoSampleId: %s",
                correctedCmoSampleView.getId()));

        String sampleClassAbbr = sampleTypeAbbreviationRetriever.retrieve(correctedCmoSampleView);
        int sampleCount = sampleCounterRetriever.retrieve(cmoSampleViews, sampleClassAbbr);

        return new PatientCmoSampleId(
                correctedCmoSampleView.getPatientId(),
                sampleClassAbbr,
                sampleCount,
                CspaceSampleAbbreviationRetriever.getNucleicAcidAbbr(correctedCmoSampleView));
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
