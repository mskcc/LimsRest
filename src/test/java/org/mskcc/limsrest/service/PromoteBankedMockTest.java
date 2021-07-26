package org.mskcc.limsrest.service;

import com.google.common.collect.ImmutableMap;
import com.velox.api.datarecord.DataRecord;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.mskcc.domain.sample.BankedSample;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 *
 * @author Fahimeh Mirhaj
 *
 * */
@RunWith(MockitoJUnitRunner.class)
public class PromoteBankedMockTest {
    @Mock
    BankedSample bankedSample;




    @InjectMocks
    private PromoteBanked promoteBanked = new PromoteBanked();

    @Before
    public void initializeMockito() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void setSeqRequirementsWES_whenRequestReadsIsValid() {
        when(bankedSample.getRequestedReads()).thenReturn("100X");
        Map<String, Object> seqRequirementMap = promoteBanked.getCovReadsRequirementsMap_forWES(bankedSample.getRequestedReads());
        Assert.assertEquals(2, seqRequirementMap.size());
        Assertions.assertThat(seqRequirementMap).containsKeys("CoverageTarget", "RequestedReads");
        Assertions.assertThat(seqRequirementMap).containsEntry("CoverageTarget", 100);
        Assertions.assertThat(seqRequirementMap).containsEntry("RequestedReads", 60.0);




    }
    @Test
    public void setSeqRequirementWES_whenCoverageTargetIsNotInMap() {
        when(bankedSample.getRequestedReads()).thenReturn("1000X");
        Map<String, Object> seqRequirementMap = promoteBanked.getCovReadsRequirementsMap_forWES(bankedSample.getRequestedReads());
        Assertions.assertThat(seqRequirementMap).hasSize(2);
        Assertions.assertThat(seqRequirementMap).containsKeys("CoverageTarget", "RequestedReads");
        Assertions.assertThat(seqRequirementMap).containsEntry("CoverageTarget", 1000);
        Assertions.assertThat(seqRequirementMap).containsEntry("RequestedReads", null);

    }
    @Test
    public void unaliquotname_whenSampleNameIsNotAliquot() {
        Assertions.assertThat(promoteBanked.unaliquotName("1234_S_1_1_1_1")).isEqualTo("1234_S_1");
    }
    @Test
    public void unaliquotname_whenSampleNameIsAliquot() {

    }
    @Test
    public void getCorrectedCmoSampleId_whenSpeciesIsNotHuman() {
        Map<String, Object> fields = ImmutableMap.<String, Object>builder()
                .put("CMOPatientId", "pid343")
                .put("SampleType", "DNA")
                .put("Species", "Mouse")
                .build();
        BankedSample bankedSample = new BankedSample("123", fields);
        String id = promoteBanked.getCorrectedCmoSampleId(bankedSample, "123_S");
        Assertions.assertThat(id).isEmpty();
    }
    @Test
    public void whenSpeciesIsHumanOrRecipeIsTreatedAsHuman_shouldGenerateCmoSampleId() {

    }
    @Test
    public void whenRecipeIsNotTreatedAsHumanOrEmpty_shouldNotGenerateCmoSampleId() {

    }
    @Test
    public void getCmoFields() {

    }
    private void assertShouldGenerateCmoId(String species, String recipe, boolean expected) {
        //given
        Map<String, Object> fields = ImmutableMap.<String, Object>builder()
                .put("CMOPatientId", "pid343")
                .put("SampleType", "DNA")
                .put("Species", species)
                .put("Recipe", recipe)
                .put("UserSampleID", "U2343")
                .put("OtherSampleId", "O34234")
                .put("SpecimenType", "Resection")
                .build();
        BankedSample bankedSample = new BankedSample("123", fields);

        //when
        boolean result = PromoteBanked.shouldGenerateCmoId(bankedSample);

        //then
        Assertions.assertThat(result).isEqualTo(expected);
    }


}
