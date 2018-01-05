package org.mskcc.limsrest.limsapi.cmoinfo.oldformat;

import org.junit.Test;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.domain.sample.SpecimenType;
import org.mskcc.limsrest.limsapi.cmoinfo.cspace.CspaceSampleAbbreviationRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.cspace.SpecimenTypeSampleAbbreviationResolver;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientAwareCmoSampleId;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleAbbreviationRetriever;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OldCorrectedCmoIdConverterTest {
    private SampleAbbreviationRetriever sampleAbbreviationRetriever = new CspaceSampleAbbreviationRetriever();
    private final OldCorrectedCmoIdConverter oldCorrectedCmoIdConverter = new OldCorrectedCmoIdConverter
            (sampleAbbreviationRetriever);

    @Test
    public void whenCountIsAtTheEndOfId_shouldReturnCorrectedCmoIdWithThisCount() throws Exception {
        //given
        String patientId = "34324_P";
        NucleicAcid nucleidAcid = NucleicAcid.DNA;
        String count = "1";
        String correctedCmoId = "123d-fds32-gf43-" + count;
        SpecimenType specimenType = SpecimenType.PDX;

        assertPatientCmoSampleId(patientId, nucleidAcid, Integer.parseInt(count), correctedCmoId, specimenType);
    }

    @Test
    public void whenCountIsSurroundedByOtherChars_shouldReturnCorrectedCmoIdWithThisCount() throws Exception {
        //given
        String patientId = "098439NJNJ";
        NucleicAcid nucleidAcid = NucleicAcid.RNA;
        String count = "012";
        String correctedCmoId = "123d-fds32-gf43-wq" + count + "ds";
        SpecimenType specimenType = SpecimenType.XENOGRAFT;

        assertPatientCmoSampleId(patientId, nucleidAcid, Integer.parseInt(count), correctedCmoId, specimenType);
    }

    private void assertPatientCmoSampleId(String patientId, NucleicAcid nucleidAcid, int count, String
            correctedCmoId, SpecimenType specimenType) {
        CorrectedCmoSampleView sample = new CorrectedCmoSampleView("1234");
        sample.setPatientId(patientId);
        sample.setNucleidAcid(nucleidAcid);
        sample.setSpecimenType(specimenType);
        sample.setCorrectedCmoId(correctedCmoId);

        //when
        PatientAwareCmoSampleId patientAwareCmoSampleId = oldCorrectedCmoIdConverter.convert(sample);

        //then
        assertThat(patientAwareCmoSampleId.getNucleicAcid(), is(CspaceSampleAbbreviationRetriever.getNucleicAcidAbbr
                (sample)));
        assertThat(patientAwareCmoSampleId.getPatientId(), is(sample.getPatientId()));
        assertThat(patientAwareCmoSampleId.getSampleCount(), is(count));
        assertThat(patientAwareCmoSampleId.getSampleTypeAbbr(), is(SpecimenTypeSampleAbbreviationResolver
                .getSpecimenTypeToAbbreviation().get(specimenType)));
    }

}