package org.mskcc.limsrest.service.dmp;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mskcc.limsrest.service.PromoteBanked;

public class PromoteBankedMockTest {
    private PromoteBanked promoteBanked;

    @Before
    public void setUp() {
        promoteBanked = mock(PromoteBanked.class);
    }

    @Test
    public void setSeqRequirementsWES_whenRequestReadsIsValid() {
        promoteBanked.getCovReadsRequirementsMap_forWES("100X");
        when(promoteBanked.getCovReadsRequirementsMap_forWES(anyString())).then();


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
