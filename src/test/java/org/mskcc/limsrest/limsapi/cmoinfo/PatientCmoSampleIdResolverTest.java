package org.mskcc.limsrest.limsapi.cmoinfo;

import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.BankedSample;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.domain.sample.SampleClass;
import org.mskcc.domain.sample.SampleOrigin;
import org.mskcc.domain.sample.SpecimenType;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.PatientCmoSampleIdResolver;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.PatientSampleCountRetriever;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mskcc.util.TestUtils.assertThrown;

public class PatientCmoSampleIdResolverTest {
    private PatientCmoSampleIdResolver patientCmoSampleIdResolver;
    private BankedSample bankedSample;
    private PatientSampleCountRetriever countRetriever = mock(PatientSampleCountRetriever.class);

    @Before
    public void setUp() throws Exception {
        patientCmoSampleIdResolver = new PatientCmoSampleIdResolver(countRetriever);
        bankedSample = new BankedSample();
        when(countRetriever.retrieve(any(), any())).thenReturn(1l);
    }

    @Test
    public void whenPatientIdIsNull_shouldThrowAnException() throws Exception {
        //given
        bankedSample.setSampleClass(SampleClass.UNKNOWN_TUMOR.getValue());
        bankedSample.setNucleicAcidType(NucleicAcid.DNA.getValue());

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(bankedSample, Collections.emptyList()));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenPatientIdIsEmpty_shouldThrowAnException() throws Exception {
        //given
        bankedSample.setPatientId("");
        bankedSample.setSampleClass(SampleClass.UNKNOWN_TUMOR.getValue());
        bankedSample.setNucleicAcidType(NucleicAcid.DNA.getValue());

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(bankedSample, Collections.emptyList()));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenSampleClassIsNull_shouldThrowAnException() throws Exception {
        //given
        bankedSample.setPatientId(getRandomPatientId());
        bankedSample.setNucleicAcidType(NucleicAcid.DNA.getValue());

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(bankedSample, Collections.emptyList()));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenSampleClassIsEmpty_shouldThrowAnException() throws Exception {
        //given
        bankedSample.setPatientId(getRandomPatientId());
        bankedSample.setSampleClass("");
        bankedSample.setNucleicAcidType(NucleicAcid.DNA.getValue());

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(bankedSample, Collections.emptyList()));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenNucleicAcidIsNull_shouldThrowAnException() throws Exception {
        //given
        bankedSample.setPatientId(getRandomPatientId());
        bankedSample.setSampleClass(SampleClass.ADJACENT_TISSUE.getValue());

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(bankedSample, Collections.emptyList()));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenNucleicAcidIsEmpty_shouldThrowAnException() throws Exception {
        //given
        bankedSample.setPatientId(getRandomPatientId());
        bankedSample.setSampleClass(SampleClass.ADJACENT_TISSUE.getValue());
        bankedSample.setNucleicAcidType("");

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(bankedSample, Collections.emptyList()));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    private String getRandomPatientId() {
        return UUID.randomUUID().toString();
    }

    @Test
    public void whenSpecimenTypeIsXenograft_shouldCreateCmoSampleIdWithX() throws Exception {
        assertCmoSampleIdBySpecimen(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.XENOGRAFT);
        assertCmoSampleIdBySpecimen(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.PDX);
        assertCmoSampleIdBySpecimen(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.XENOGRAFTDERIVEDCELLLINE);
    }

    @Test
    public void whenSpecimenTypeIsOrganoid_shouldCreateCmoSampleIdWithG() throws Exception {
        assertCmoSampleIdBySpecimen(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.ORGANOID);
    }

    @Test
    public void whenSpecimenTypeIsCfdna_shouldCreateCmoSampleIdBySampleOrigin() throws Exception {
        assertCmoSampleIdBySpecimenByOrigin(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.CFDNA, SampleOrigin.URINE);
        assertCmoSampleIdBySpecimenByOrigin(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.CFDNA, SampleOrigin.CEREBROSPINAL_FLUID);
        assertCmoSampleIdBySpecimenByOrigin(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.CFDNA, SampleOrigin.PLASMA);
        assertCmoSampleIdBySpecimenByOrigin(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.CFDNA, SampleOrigin.WHOLE_BLOOD);
    }

    private void assertCmoSampleIdBySpecimenByOrigin(String patientId, NucleicAcid nucleicAcid, SpecimenType specimenType, SampleOrigin sampleOrigin) {
        //given
        BankedSample sample = new BankedSample();
        sample.setPatientId(patientId);
        sample.setSpecimenType(specimenType.getValue());
        sample.setNucleicAcidType(nucleicAcid.getValue());
        sample.setSampleOrigin(sampleOrigin.getValue());

        //when
        PatientCmoSampleId cmoSampleId = patientCmoSampleIdResolver.resolve(sample, Collections.emptyList());

        //then
        assertThat(cmoSampleId.getPatientId(), is(patientId));
        assertThat(cmoSampleId.getSampleTypeAbbr(), is(PatientCmoSampleIdResolver.getSampleOriginToAbbreviation().get(sampleOrigin)));
        assertThat(cmoSampleId.getSampleCount(), is(1l));
        assertThat(cmoSampleId.getNucleicAcid(), is(PatientCmoSampleIdResolver.getNucleicAcidToAbbreviation().get(nucleicAcid)));
    }

    @Test
    public void whenSpecimenTypeIsNotCelllineNorXenograftNorOrganoidNorCfDna_shouldCreateCmoSampleIdBySampleClass() throws Exception {
        assertCmoSampleIdBySampleClass(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.BIOPSY, SampleClass.UNKNOWN_TUMOR);
        assertCmoSampleIdBySampleClass(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.BLOOD, SampleClass.LOCAL_RECURRENCE);
        assertCmoSampleIdBySampleClass(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.FINGERNAILS, SampleClass.PRIMARY);
        assertCmoSampleIdBySampleClass(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.RAPIDAUTOPSY, SampleClass.METASTASIS);
        assertCmoSampleIdBySampleClass(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.OTHER, SampleClass.NORMAL);
        assertCmoSampleIdBySampleClass(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.RESECTION, SampleClass.ADJACENT_NORMAL);
        assertCmoSampleIdBySampleClass(getRandomPatientId(), NucleicAcid.DNA, SpecimenType.SALIVA, SampleClass.ADJACENT_TISSUE);
    }

    private void assertCmoSampleIdBySampleClass(String patientId, NucleicAcid nucleicAcid, SpecimenType specimenType, SampleClass sampleClass) {
        //given
        BankedSample sample = new BankedSample();
        sample.setPatientId(patientId);
        sample.setSpecimenType(specimenType.getValue());
        sample.setNucleicAcidType(nucleicAcid.getValue());
        sample.setSampleClass(sampleClass.getValue());

        //when
        PatientCmoSampleId cmoSampleId = patientCmoSampleIdResolver.resolve(sample, Collections.emptyList());

        //then
        assertThat(cmoSampleId.getPatientId(), is(patientId));
        assertThat(cmoSampleId.getSampleTypeAbbr(), is(PatientCmoSampleIdResolver.getSampleClassToAbbreviation().get(sampleClass)));
        assertThat(cmoSampleId.getSampleCount(), is(1l));
        assertThat(cmoSampleId.getNucleicAcid(), is(PatientCmoSampleIdResolver.getNucleicAcidToAbbreviation().get(nucleicAcid)));
    }

    private void assertCmoSampleIdBySpecimen(String patientId, NucleicAcid nucleicAcid, SpecimenType specimenType) {
        //given
        BankedSample sample = new BankedSample();
        sample.setPatientId(patientId);
        sample.setSpecimenType(specimenType.getValue());
        sample.setNucleicAcidType(nucleicAcid.getValue());

        //when
        PatientCmoSampleId cmoSampleId = patientCmoSampleIdResolver.resolve(sample, Collections.emptyList());

        //then
        assertThat(cmoSampleId.getPatientId(), is(patientId));
        assertThat(cmoSampleId.getSampleTypeAbbr(), is(PatientCmoSampleIdResolver.getSpecimenTypeToAbbreviation().get(specimenType)));
        assertThat(cmoSampleId.getSampleCount(), is(1l));
        assertThat(cmoSampleId.getNucleicAcid(), is(PatientCmoSampleIdResolver.getNucleicAcidToAbbreviation().get(nucleicAcid)));
    }

    @Test
    public void whenSampleClassIsCellFreeAndSampleOriginIsNull_shouldThrowAnException() throws Exception {
        //given
        String patientId = getRandomPatientId();
        String cellFreeSampleClass = SampleClass.CELL_FREE.getValue();
        String nucleicAcid = NucleicAcid.DNA.getValue();

        bankedSample.setPatientId(patientId);
        bankedSample.setSampleClass(cellFreeSampleClass);
        bankedSample.setNucleicAcidType(nucleicAcid);

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(bankedSample, Collections.emptyList()));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenSampleClassIsCellFreeAndSampleOriginIsEmpty_shouldThrowAnException() throws Exception {
        //given
        String patientId = getRandomPatientId();
        String cellFreeSampleClass = SampleClass.CELL_FREE.getValue();
        String nucleicAcid = NucleicAcid.DNA.getValue();

        bankedSample.setPatientId(patientId);
        bankedSample.setSampleClass(cellFreeSampleClass);
        bankedSample.setNucleicAcidType(nucleicAcid);
        bankedSample.setSampleOrigin("");

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(bankedSample, Collections.emptyList()));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenSampleIsCellFree_sampleClassShouldBeRetrievedFromSampleOrigin() throws Exception {
        //given
        String patientId = getRandomPatientId();
        SampleClass sampleClass = SampleClass.CELL_FREE;
        SampleOrigin sampleOrigin = SampleOrigin.CEREBROSPINAL_FLUID;
        NucleicAcid nucleicAcid = NucleicAcid.DNA;

        bankedSample.setPatientId(patientId);
        bankedSample.setSampleClass(sampleClass.getValue());
        bankedSample.setNucleicAcidType(nucleicAcid.getValue());
        bankedSample.setSampleOrigin(sampleOrigin.getValue());
        bankedSample.setSpecimenType(SpecimenType.BIOPSY.getValue());

        //when
        PatientCmoSampleId cmoSampleId = patientCmoSampleIdResolver.resolve(bankedSample, Collections.emptyList());

        //then
        String sampleClassByOrigin = PatientCmoSampleIdResolver.getSampleOriginToAbbreviation().get(sampleOrigin);
        String nucleicAcidShortcut = PatientCmoSampleIdResolver.getNucleicAcidToAbbreviation().get(nucleicAcid);

        assertThat(cmoSampleId.getPatientId(), is(patientId));
        assertThat(cmoSampleId.getSampleTypeAbbr(), is(sampleClassByOrigin));
        assertThat(cmoSampleId.getSampleCount(), is(1l));
        assertThat(cmoSampleId.getNucleicAcid(), is(nucleicAcidShortcut));
    }

}

