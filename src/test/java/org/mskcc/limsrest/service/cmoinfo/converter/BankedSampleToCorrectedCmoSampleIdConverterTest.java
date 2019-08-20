package org.mskcc.limsrest.service.cmoinfo.converter;

import org.junit.Test;
import org.mskcc.domain.Recipe;
import org.mskcc.domain.sample.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BankedSampleToCorrectedCmoSampleIdConverterTest {
    private BankedSampleToCorrectedCmoSampleIdConverter bankedSampleToCorrectedCmoSampleIdConverter = new
            BankedSampleToCorrectedCmoSampleIdConverter();

    @Test
    public void whenBankedSampleFieldsAreFilledIn_shouldBeWrittenInCorrectedCmoSampleView() throws Exception {
        //given
        String id = "id";
        String userSampleId = "userSampId";
        String cmoPatId = "cmoPatId";
        String reqId = "reqId";
        NucleicAcid naToExtract = NucleicAcid.CFDNA;
        Recipe recipe = Recipe.AMPLI_SEQ;
        SampleType sampleType = SampleType.RNA;
        SpecimenType specimenType = SpecimenType.BLOOD;
        SampleClass sampleClass = SampleClass.ADJACENT_TISSUE;
        SampleOrigin sampleOrigin = SampleOrigin.PLASMA;

        BankedSample bankedSample = new BankedSample(id);
        bankedSample.setUserSampleID(userSampleId);
        bankedSample.setCMOPatientId(cmoPatId);
        bankedSample.setRequestId(reqId);
        bankedSample.setNAtoExtract(naToExtract.getValue());
        bankedSample.setRecipe(recipe.getValue());
        bankedSample.setSampleType(sampleType.toString());
        bankedSample.setSpecimenType(specimenType.getValue());
        bankedSample.setSampleClass(sampleClass.getValue());
        bankedSample.setSampleOrigin(sampleOrigin.getValue());

        //when
        CorrectedCmoSampleView correctedCmoSampleView = bankedSampleToCorrectedCmoSampleIdConverter.convert
                (bankedSample);

        //then
        assertThat(correctedCmoSampleView.getId(), is(userSampleId));
        assertThat(correctedCmoSampleView.getSampleId(), is(userSampleId));
        assertThat(correctedCmoSampleView.getPatientId(), is(cmoPatId));
        assertThat(correctedCmoSampleView.getRequestId(), is(reqId));
        assertThat(correctedCmoSampleView.getNucleidAcid(), is(naToExtract));
        assertThat(correctedCmoSampleView.getSampleType(), is(sampleType));
        assertThat(correctedCmoSampleView.getSpecimenType(), is(specimenType));
        assertThat(correctedCmoSampleView.getSampleClass(), is(sampleClass));
        assertThat(correctedCmoSampleView.getSampleOrigin(), is(sampleOrigin));
    }
}