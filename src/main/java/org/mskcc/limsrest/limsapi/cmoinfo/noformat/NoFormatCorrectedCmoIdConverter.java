package org.mskcc.limsrest.limsapi.cmoinfo.noformat;

import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.StringToSampleCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.cspace.CspaceSampleAbbreviationRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleId;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleAbbreviationRetriever;

public class NoFormatCorrectedCmoIdConverter implements StringToSampleCmoIdConverter {
    private final SampleAbbreviationRetriever sampleTypeAbbreviationRetriever;

    public NoFormatCorrectedCmoIdConverter(SampleAbbreviationRetriever sampleTypeAbbreviationRetriever) {
        this.sampleTypeAbbreviationRetriever = sampleTypeAbbreviationRetriever;
    }

    @Override
    public PatientCmoSampleId convert(CorrectedCmoSampleView correctedCmoSampleView) {
        String patientId = correctedCmoSampleView.getPatientId();
        String sampleClass = sampleTypeAbbreviationRetriever.retrieve(correctedCmoSampleView);
        int sampleCount = 0;
        String nucleicAcid = CspaceSampleAbbreviationRetriever.getNucleicAcidAbbr(correctedCmoSampleView);

        PatientCmoSampleId patientCmoSampleId = new PatientCmoSampleId(patientId, sampleClass, sampleCount,
                nucleicAcid);
        return patientCmoSampleId;
    }
}
