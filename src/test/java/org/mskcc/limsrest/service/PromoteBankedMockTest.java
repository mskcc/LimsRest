package org.mskcc.limsrest.service;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

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
    private Map<String, Object> seqRequirementMap;

    @Mock



    @InjectMocks
    private PromoteBanked promoteBanked = new PromoteBanked();

    @Before
    public void initializeMockito() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void setSeqRequirementsWES_whenRequestReadsIsValid() {
        Map<String, Object> seqReqMap = ImmutableMap.<String, Object>builder()
        .put("30", 20.0)
        .put("70", 45.0)
        .put("100", 60.0)
        .put("150", 95.0)
        .put("200", 120.0)
        .put("250", 160.0)
        .build();

        when(seqReqMap.size()).thenReturn();

        Assert.assertEquals(6, seqRequirementMap.size());
        Assertions.assertThat(seqRequirementMap).containsKeys("CoverageTarget", "RequestedReads");
        Assertions.assertThat(seqRequirementMap).containsEntry("CoverageTarget", 100);
        Assertions.assertThat(seqRequirementMap).containsEntry("RequestedReads", 60.0);
        //verify(promoteBanked).getCovReadsRequirementsMap_forWES("100X");

    }
    @Test
    public void setSeqRequirementWES_whenCoverageTargetIsNotInMap() {

    }
    @Test
    public void unaliquotname_whenSampleNameIsNotAliquot() {

    }
    @Test
    public void unaliquotname_whenSampleNameIsAliquot() {

    }
    @Test
    public void getCorrectedCmoSampleId_whenSpeciesIsNotHuman() {

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

}
