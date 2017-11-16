package org.mskcc.limsrest.limsapi.cmoinfo;

import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.domain.sample.SampleClass;
import org.mskcc.domain.sample.SampleOrigin;
import org.mskcc.domain.sample.SpecimenType;
import org.mskcc.limsrest.limsapi.cmoinfo.cspace.CspaceSampleAbbreviationRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleId;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleIdResolver;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.IncrementalSampleCounterRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.SampleAbbreviationRetriever;

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
    private CorrectedCmoSampleView sample;
    private IncrementalSampleCounterRetriever countRetriever = mock(IncrementalSampleCounterRetriever.class);
    private String requestId = "6543w2_O";
    private String sampleId = "s43222";
    private SampleAbbreviationRetriever sampleTypeAbbreviationRetriever = new CspaceSampleAbbreviationRetriever();

    @Before
    public void setUp() throws Exception {
        patientCmoSampleIdResolver = new PatientCmoSampleIdResolver(countRetriever, sampleTypeAbbreviationRetriever);
        sample = new CorrectedCmoSampleView(sampleId);
        when(countRetriever.retrieve(any(), any())).thenReturn(1);
    }

    @Test
    public void whenPatientIdIsNull_shouldThrowAnException() throws Exception {
        //given
        sample.setSampleClass(SampleClass.UNKNOWN_TUMOR);
        sample.setNucleidAcid(NucleicAcid.DNA);

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(sample, Collections
                .emptyList(), requestId));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenPatientIdIsEmpty_shouldThrowAnException() throws Exception {
        //given
        sample.setPatientId("");
        sample.setSampleClass(SampleClass.UNKNOWN_TUMOR);
        sample.setNucleidAcid(NucleicAcid.DNA);

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(sample, Collections
                .emptyList(), requestId));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenSampleClassIsNull_shouldThrowAnException() throws Exception {
        //given
        sample.setPatientId(getRandomPatientId());
        sample.setNucleidAcid(NucleicAcid.DNA);

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(sample, Collections.emptyList(), requestId));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenSampleClassIsEmpty_shouldThrowAnException() throws Exception {
        //given
        sample.setPatientId(getRandomPatientId());
        sample.setSampleClass(null);
        sample.setNucleidAcid(NucleicAcid.DNA);

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(sample, Collections.emptyList(), requestId));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenNucleicAcidIsNull_shouldThrowAnException() throws Exception {
        //given
        sample.setPatientId(getRandomPatientId());
        sample.setSampleClass(SampleClass.ADJACENT_TISSUE);

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(sample, Collections.emptyList(), requestId));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenNucleicAcidIsEmpty_shouldThrowAnException() throws Exception {
        //given
        sample.setPatientId(getRandomPatientId());
        sample.setSampleClass(SampleClass.ADJACENT_TISSUE);
        sample.setNucleidAcid(null);

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(sample, Collections.emptyList(), requestId));

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
        CorrectedCmoSampleView sample = new CorrectedCmoSampleView("sampleId");
        sample.setPatientId(patientId);
        sample.setSpecimenType(specimenType);
        sample.setNucleidAcid(nucleicAcid);
        sample.setSampleOrigin(sampleOrigin);

        //when
        PatientCmoSampleId cmoSampleId = patientCmoSampleIdResolver.resolve(sample, Collections.emptyList(), requestId);

        //then
        assertThat(cmoSampleId.getPatientId(), is(patientId));
        assertThat(cmoSampleId.getSampleTypeAbbr(), is(CspaceSampleAbbreviationRetriever
                .getSampleOriginToAbbreviation().get(sampleOrigin)));
        assertThat(cmoSampleId.getSampleCount(), is(1));
        assertThat(cmoSampleId.getNucleicAcid(), is(CspaceSampleAbbreviationRetriever.getNucleicAcidToAbbreviation().get(nucleicAcid)));
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

    private void assertCmoSampleIdBySampleClass(String patientId, NucleicAcid nucleicAcid, SpecimenType specimenType,
                                                SampleClass sampleClass) {
        //given
        CorrectedCmoSampleView sample = new CorrectedCmoSampleView("sampleId");
        sample.setPatientId(patientId);
        sample.setSpecimenType(specimenType);
        sample.setNucleidAcid(nucleicAcid);
        sample.setSampleClass(sampleClass);

        //when
        PatientCmoSampleId cmoSampleId = patientCmoSampleIdResolver.resolve(sample, Collections.emptyList(), requestId);

        //then
        assertThat(cmoSampleId.getPatientId(), is(patientId));
        assertThat(cmoSampleId.getSampleTypeAbbr(), is(CspaceSampleAbbreviationRetriever.getSampleClassToAbbreviation
                ().get(sampleClass)));
        assertThat(cmoSampleId.getSampleCount(), is(1));
        assertThat(cmoSampleId.getNucleicAcid(), is(CspaceSampleAbbreviationRetriever.getNucleicAcidToAbbreviation().get(nucleicAcid)));
    }

    private void assertCmoSampleIdBySpecimen(String patientId, NucleicAcid nucleicAcid, SpecimenType specimenType) {
        //given
        CorrectedCmoSampleView sample = new CorrectedCmoSampleView("sampleId");
        sample.setPatientId(patientId);
        sample.setSpecimenType(specimenType);
        sample.setNucleidAcid(nucleicAcid);

        //when
        PatientCmoSampleId cmoSampleId = patientCmoSampleIdResolver.resolve(sample, Collections.emptyList(), requestId);

        //then
        assertThat(cmoSampleId.getPatientId(), is(patientId));
        assertThat(cmoSampleId.getSampleTypeAbbr(), is(CspaceSampleAbbreviationRetriever
                .getSpecimenTypeToAbbreviation().get(specimenType)));
        assertThat(cmoSampleId.getSampleCount(), is(1));
        assertThat(cmoSampleId.getNucleicAcid(), is(CspaceSampleAbbreviationRetriever.getNucleicAcidToAbbreviation().get(nucleicAcid)));
    }

    @Test
    public void whenSampleClassIsCellFreeAndSampleOriginIsNull_shouldThrowAnException() throws Exception {
        //given
        String patientId = getRandomPatientId();
        SampleClass cellFreeSampleClass = SampleClass.CELL_FREE;
        NucleicAcid nucleicAcid = NucleicAcid.DNA;

        sample.setPatientId(patientId);
        sample.setSampleClass(cellFreeSampleClass);
        sample.setNucleidAcid(nucleicAcid);

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(sample, Collections.emptyList(), requestId));

        //then
        assertThat(exception.isPresent(), is(true));
    }

    @Test
    public void whenSampleClassIsCellFreeAndSampleOriginIsEmpty_shouldThrowAnException() throws Exception {
        //given
        String patientId = getRandomPatientId();
        SampleClass cellFreeSampleClass = SampleClass.CELL_FREE;
        NucleicAcid nucleicAcid = NucleicAcid.DNA;

        sample.setPatientId(patientId);
        sample.setSampleClass(cellFreeSampleClass);
        sample.setNucleidAcid(nucleicAcid);
        sample.setSampleOrigin(null);

        //when
        Optional<Exception> exception = assertThrown(() -> patientCmoSampleIdResolver.resolve(sample, Collections.emptyList(), requestId));

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

        sample.setPatientId(patientId);
        sample.setSampleClass(sampleClass);
        sample.setNucleidAcid(nucleicAcid);
        sample.setSampleOrigin(sampleOrigin);
        sample.setSpecimenType(SpecimenType.BIOPSY);

        //when
        PatientCmoSampleId cmoSampleId = patientCmoSampleIdResolver.resolve(sample, Collections.emptyList(), requestId);

        //then
        String sampleClassByOrigin = CspaceSampleAbbreviationRetriever.getSampleOriginToAbbreviation().get(sampleOrigin);
        String nucleicAcidShortcut = CspaceSampleAbbreviationRetriever.getNucleicAcidToAbbreviation().get(nucleicAcid);

        assertThat(cmoSampleId.getPatientId(), is(patientId));
        assertThat(cmoSampleId.getSampleTypeAbbr(), is(sampleClassByOrigin));
        assertThat(cmoSampleId.getSampleCount(), is(1));
        assertThat(cmoSampleId.getNucleicAcid(), is(nucleicAcidShortcut));
    }

}

