package org.mskcc.limsrest.service.cmorequests;

import org.junit.jupiter.api.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class CheckOrMarkCmoRequestsTaskTests {

    @Test
    public void isCmoRequest_test(){
        assertTrue(CheckOrMarkCmoRequestsTask.isCmoRequest("skicmopm@mskcc.org", "IMPACT468", true));
        assertTrue(CheckOrMarkCmoRequestsTask.isCmoRequest("johndoe@mskcc.org", "IMPACT468", true));
        assertFalse(CheckOrMarkCmoRequestsTask.isCmoRequest("johndoe@mskcc.org", "ChipSeq", true));
        assertFalse(CheckOrMarkCmoRequestsTask.isCmoRequest("johndoe@mskcc.org", "ChipSeq", false));
    }

    @Test
    public void isWesCapture_test(){
        assertTrue(CheckOrMarkCmoRequestsTask.isWesRecapture("WholeExomeSequencing", "DNA Library", "P-0007373-T01-WES"));
        assertFalse(CheckOrMarkCmoRequestsTask.isWesRecapture("RNASeq_RiboDeplete", "DNA Library", "P-0007373-T01-WES"));
    }

    @Test
    public void isCmoAnalystEmail_test(){
        assertTrue(CheckOrMarkCmoRequestsTask.isCmoAnalystEmail("bergerm1@mskcc.org"));
        assertFalse(CheckOrMarkCmoRequestsTask.isCmoAnalystEmail("johndoe@mskcc.org"));
    }

    @Test
    public void isCmoRecipe_test(){
        assertTrue(CheckOrMarkCmoRequestsTask.isCmoRecipe("IMPACT468", false));
        assertTrue(CheckOrMarkCmoRequestsTask.isCmoRecipe("MSK-ACCESS_v1", false));
        assertTrue(CheckOrMarkCmoRequestsTask.isCmoRecipe("MSK-ACCESS_v1", true));
        assertTrue(CheckOrMarkCmoRequestsTask.isCmoRecipe("WholeExomeSequencing", true));
        assertFalse(CheckOrMarkCmoRequestsTask.isCmoRecipe("WholeExomeSequencing", false));
        assertFalse(CheckOrMarkCmoRequestsTask.isCmoRecipe("RNASeq_RiboDeplete", true));
        assertFalse(CheckOrMarkCmoRequestsTask.isCmoRecipe("RNASeq_RiboDeplete", false));
        assertFalse(CheckOrMarkCmoRequestsTask.isCmoRecipe("M-IMPACT_v1", false));
    }
}