package org.mskcc.limsrest.limsapi.cmoinfo.noformat;

import org.junit.Test;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.domain.sample.SampleClass;
import org.mskcc.domain.sample.SpecimenType;
import org.mskcc.limsrest.limsapi.cmoinfo.cspace.CspaceSampleAbbreviationRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleId;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleAbbreviationRetriever;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NoFormatCorrectedCmoIdConverterTest {
    private SampleAbbreviationRetriever sampleAbbreviationRetriever = new CspaceSampleAbbreviationRetriever();
    private NoFormatCorrectedCmoIdConverter noFormatCorrectedCmoIdConverter = new NoFormatCorrectedCmoIdConverter
            (sampleAbbreviationRetriever);

    @Test
    public void whenSampleCmoIdHasNoProperFormat_shouldReturnCmoSampleIdWithCount0() throws Exception {
        assertCmoSampleId("p1234", NucleicAcid.DNA, SampleClass.ADJACENT_NORMAL.getValue());
        assertCmoSampleId("0123456", NucleicAcid.DNA, "");
        assertCmoSampleId("p1234", NucleicAcid.RNA, "");
    }

    private void assertCmoSampleId(String patientId, NucleicAcid nAtoExtract, String correctedCmoClass) {
        CorrectedCmoSampleView sample = new CorrectedCmoSampleView("sampleId");

        sample.setPatientId(patientId);
        sample.setCorrectedCmoId(correctedCmoClass);
        sample.setNucleidAcid(nAtoExtract);
        sample.setSpecimenType(SpecimenType.XENOGRAFT);

        PatientCmoSampleId cmoSampleId = noFormatCorrectedCmoIdConverter.convert(sample);

        assertThat(cmoSampleId.getPatientId(), is(patientId));
        assertThat(cmoSampleId.getNucleicAcid(), is(CspaceSampleAbbreviationRetriever.getNucleicAcidAbbr(sample)));
        assertThat(cmoSampleId.getSampleCount(), is(0));
        assertThat(cmoSampleId.getSampleTypeAbbr(), is("X"));
    }
}