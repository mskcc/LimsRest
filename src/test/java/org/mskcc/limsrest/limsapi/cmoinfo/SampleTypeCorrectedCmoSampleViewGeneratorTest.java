package org.mskcc.limsrest.limsapi.cmoinfo;

import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.domain.sample.SpecimenType;
import org.mskcc.limsrest.limsapi.PatientSamplesRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.cellline.CellLineCmoSampleIdFormatter;
import org.mskcc.limsrest.limsapi.cmoinfo.cellline.CellLineCmoSampleIdResolver;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.CorrectedCmoIdConverterFactory;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.FormatAwareCorrectedCmoIdConverterFactory;
import org.mskcc.limsrest.limsapi.cmoinfo.cspace.CspaceSampleTypeAbbreviationRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.formatter.CmoSampleIdFormatter;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleIdFormatter;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleIdResolver;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SampleTypeCorrectedCmoSampleViewGeneratorTest {
    private final PatientSamplesRetriever samplesRetriever = mock(PatientSamplesRetriever.class);

    private SampleTypeCorrectedCmoSampleIdGenerator sampleTypeCorrectedCmoSampleIdGenerator;
    private DataRecordManager drm = mock(DataRecordManager.class);
    private User user = mock(User.class);
    private CmoSampleIdRetrieverFactory retrieverFactory;
    private SampleTypeAbbreviationRetriever abbrRetriever = new CspaceSampleTypeAbbreviationRetriever();
    private CorrectedCmoIdConverterFactory convFactory = new FormatAwareCorrectedCmoIdConverterFactory(abbrRetriever);

    @Before
    public void setUp() throws Exception {
        SampleCounterRetriever counterRetriever = new IncrementalSampleCounterRetriever(convFactory);
        CmoSampleIdResolver patientResolver = new PatientCmoSampleIdResolver(counterRetriever, abbrRetriever);
        CmoSampleIdFormatter patientFormatter = new PatientCmoSampleIdFormatter();
        CmoSampleIdRetriever patientRetriever = new FormattedCmoSampleIdRetriever(patientResolver, patientFormatter);
        CmoSampleIdResolver cellLineResolver = new CellLineCmoSampleIdResolver();
        CmoSampleIdFormatter cellLineFormatter = new CellLineCmoSampleIdFormatter();
        CmoSampleIdRetriever cellLineRetriever = new FormattedCmoSampleIdRetriever(cellLineResolver, cellLineFormatter);
        retrieverFactory = new CmoSampleIdRetrieverFactory(patientRetriever, cellLineRetriever);

        sampleTypeCorrectedCmoSampleIdGenerator = new SampleTypeCorrectedCmoSampleIdGenerator();
    }

    @Test
    public void whenThereAreNoPatientSamples_shouldReturnCmoIdWithNumber1() throws Exception {
        when(samplesRetriever.retrieve(any(), any(), any())).thenReturn(Collections.emptyList());
        CorrectedCmoSampleView sample = getSample("4324", "C-1235", SpecimenType.XENOGRAFT, NucleicAcid.DNA);

        String cmoId = sampleTypeCorrectedCmoSampleIdGenerator.generate(sample, "5432_P", drm, user);

        assertThat(cmoId, is("C-1235-X001-d"));
    }

    @Test
    public void whenThereIsOnePatientSampleFromSameRequestWithCount1_shouldReturnCmoIdWithNumber2() throws Exception {
        String requestId = "5432_P";
        when(samplesRetriever.retrieve(any(), any(), any())).thenReturn(Arrays.asList(getSample("C-1235-X001-d", requestId)));
        sampleTypeCorrectedCmoSampleIdGenerator.patientSamplesRetriever = samplesRetriever;

        CorrectedCmoSampleView sample = getSample("4324", "C-1235", SpecimenType.XENOGRAFT, NucleicAcid.DNA);

        String cmoId = sampleTypeCorrectedCmoSampleIdGenerator.generate(sample, requestId, drm, user);

        assertThat(cmoId, is("C-1235-X002-d"));
    }

    @Test
    public void whenThereIsOnePatientSampleFromSameRequestWithSomeCount_shouldReturnCmoIdWithThisCountPlusOne()
            throws Exception {
        String requestId = "5432_P";
        when(samplesRetriever.retrieve(any(), any(), any())).thenReturn(Arrays.asList(getSample("C-1235-X012-d", requestId)));
        sampleTypeCorrectedCmoSampleIdGenerator.patientSamplesRetriever = samplesRetriever;
        CorrectedCmoSampleView sample = getSample("4324", "C-1235", SpecimenType.XENOGRAFT, NucleicAcid.DNA);


        String cmoId = sampleTypeCorrectedCmoSampleIdGenerator.generate(sample, requestId, drm, user);

        //then
        assertThat(cmoId, is("C-1235-X013-d"));
    }

    @Test
    public void whenThereIsOnePatientSampleFromDifferentRequest_shouldReturnCmoIdWithNumber2() throws Exception {
        when(samplesRetriever.retrieve(any(), any(), any())).thenReturn(Arrays.asList(getSample("C-1235-X001-d",
                "1234_A")));
        sampleTypeCorrectedCmoSampleIdGenerator.patientSamplesRetriever = samplesRetriever;
        CorrectedCmoSampleView sample = getSample("4324", "C-1235", SpecimenType.PDX, NucleicAcid.RNA);

        //when
        String cmoId = sampleTypeCorrectedCmoSampleIdGenerator.generate(sample, "5432_P", drm, user);

        //then
        assertThat(cmoId, is("C-1235-X002-r"));
    }

    @Test
    public void
    whenThereAreNoPatientSamplesButOneWasAlreadyCreatedFromSameRequestSameSpecimenSampleNucl_shouldReturnCmoIdWithNumber2() throws Exception {
        //given
        when(samplesRetriever.retrieve(any(), any(), any())).thenReturn(new ArrayList<>());
        String patientId = "C-1235";
        String requestId = "5432_P";
        CorrectedCmoSampleView sample1 = getSample("4324_1", patientId, SpecimenType.XENOGRAFT, NucleicAcid.DNA);
        CorrectedCmoSampleView sample2 = getSample("4324_2", patientId, SpecimenType.XENOGRAFT, NucleicAcid.DNA);

        String cmoId1 = sampleTypeCorrectedCmoSampleIdGenerator.generate(sample1, requestId, drm, user);

        //when
        String cmoId2 = sampleTypeCorrectedCmoSampleIdGenerator.generate(sample2, requestId, drm, user);

        //then
        assertThat(cmoId1, is("C-1235-X001-d"));
        assertThat(cmoId2, is("C-1235-X002-d"));
    }

    @Test
    public void
    whenThereAreNoPatientSamplesButOneWasAlreadyCreatedFromSameRequestSameSpecimenDifferentNucl_shouldReturnCmoIdWithNumber2() throws Exception {
        //given
        when(samplesRetriever.retrieve(any(), any(), any())).thenReturn(new ArrayList<>());
        String patientId = "C-1235";
        String requestId = "5432_P";
        CorrectedCmoSampleView sample1 = getSample("4324_1", patientId, SpecimenType.XENOGRAFT, NucleicAcid.DNA);
        CorrectedCmoSampleView sample2 = getSample("4324_2", patientId, SpecimenType.XENOGRAFT, NucleicAcid.RNA);

        String cmoId1 = sampleTypeCorrectedCmoSampleIdGenerator.generate(sample1, requestId, drm, user);

        //when
        String cmoId2 = sampleTypeCorrectedCmoSampleIdGenerator.generate(sample2, requestId, drm, user);

        //then
        assertThat(cmoId1, is("C-1235-X001-d"));
        assertThat(cmoId2, is("C-1235-X002-r"));
    }

    @Test
    public void
    whenThereAreNoPatientSamplesButOneWasAlreadyCreatedFromSameRequestSameSampleType_shouldReturnCmoIdWithNumber2()
            throws Exception {
        //given
        when(samplesRetriever.retrieve(any(), any(), any())).thenReturn(new ArrayList<>());
        String patientId = "C-1235";
        String requestId = "5432_P";
        CorrectedCmoSampleView sample1 = getSample("4324_1", patientId, SpecimenType.XENOGRAFT, NucleicAcid.DNA);
        CorrectedCmoSampleView sample2 = getSample("4324_2", patientId, SpecimenType.PDX, NucleicAcid.DNA);

        String cmoId1 = sampleTypeCorrectedCmoSampleIdGenerator.generate(sample1, requestId, drm, user);

        //when
        String cmoId2 = sampleTypeCorrectedCmoSampleIdGenerator.generate(sample2, requestId, drm, user);

        //then
        assertThat(cmoId1, is("C-1235-X001-d"));
        assertThat(cmoId2, is("C-1235-X002-d"));
    }

    @Test
    public void
    whenThereAreNoPatientSamplesButOneWasAlreadyCreatedFromDifferentRequestsSampleSpecimen_shouldReturnCmoIdWithNumber2() throws Exception {
        when(samplesRetriever.retrieve(any(), any(), any())).thenReturn(new ArrayList<>());
        String patientId = "C-1235";
        CorrectedCmoSampleView sample1 = getSample("4324_1", patientId, SpecimenType.ORGANOID, NucleicAcid.DNA);
        CorrectedCmoSampleView sample2 = getSample("4324_2", patientId, SpecimenType.ORGANOID, NucleicAcid.DNA);

        String cmoId1 = sampleTypeCorrectedCmoSampleIdGenerator.generate(sample1, "5432_P", drm, user);
        String cmoId2 = sampleTypeCorrectedCmoSampleIdGenerator.generate(sample2, "0789_R", drm, user);

        assertThat(cmoId1, is("C-1235-G001-d"));
        assertThat(cmoId2, is("C-1235-G002-d"));
    }

    private CorrectedCmoSampleView getSample(String correctedId, String reqId) {
        CorrectedCmoSampleView sample = new CorrectedCmoSampleView("5656");
        sample.setCorrectedCmoId(correctedId);
        sample.setRequestId(reqId);
        sample.setSpecimenType(SpecimenType.XENOGRAFT);

        return sample;
    }

    private CorrectedCmoSampleView getSample(String sampleId, String patientId, SpecimenType specimenType,
                                             NucleicAcid nAtoExtract) {
        CorrectedCmoSampleView sample = new CorrectedCmoSampleView(sampleId);
        sample.setPatientId(patientId);
        sample.setSpecimenType(specimenType);
        sample.setNucleidAcid(nAtoExtract);
        return sample;
    }
}