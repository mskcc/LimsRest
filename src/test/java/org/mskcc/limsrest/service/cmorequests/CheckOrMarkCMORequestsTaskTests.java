package org.mskcc.limsrest.service.cmorequests;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mskcc.domain.Recipe.*;

public class CheckOrMarkCMORequestsTaskTests {

    private CheckOrMarkCMORequestsTask checkOrMarkCMORequestsTask;

    @Test
    public void isCmoRequest_test(){
        assertTrue(CheckOrMarkCMORequestsTask.isCmoRequest("skicmopm@mskcc.org", "IMPACT468", true));
        assertTrue(CheckOrMarkCMORequestsTask.isCmoRequest("johndoe@mskcc.org", "IMPACT468", true));
        assertFalse(CheckOrMarkCMORequestsTask.isCmoRequest("johndoe@mskcc.org", "ChipSeq", true));
        assertFalse(CheckOrMarkCMORequestsTask.isCmoRequest("johndoe@mskcc.org", "ChipSeq", false));
    }

    @Test
    public void isWesCapture_test(){
        assertTrue(CheckOrMarkCMORequestsTask.isWesRecapture("WholeExomeSequencing", "DNA Library", "P-0007373-T01-WES"));
        assertFalse(CheckOrMarkCMORequestsTask.isWesRecapture("RNASeq_RiboDeplete", "DNA Library", "P-0007373-T01-WES"));
    }

    @Test
    public void isCmoAnalystEmail_test(){
        assertTrue(CheckOrMarkCMORequestsTask.isCmoAnalystEmail("bergerm1@mskcc.org"));
        assertFalse(CheckOrMarkCMORequestsTask.isCmoAnalystEmail("johndoe@mskcc.org"));
    }

    @Test
    public void isCmoRecipe_test(){
        assertTrue(CheckOrMarkCMORequestsTask.isCmoRecipe("IMPACT468", false));
        assertTrue(CheckOrMarkCMORequestsTask.isCmoRecipe("MSK-ACCESS_v1", false));
        assertTrue(CheckOrMarkCMORequestsTask.isCmoRecipe("MSK-ACCESS_v1", true));
        assertTrue(CheckOrMarkCMORequestsTask.isCmoRecipe("WholeExomeSequencing", true));
        assertFalse(CheckOrMarkCMORequestsTask.isCmoRecipe("WholeExomeSequencing", false));
        assertFalse(CheckOrMarkCMORequestsTask.isCmoRecipe("RNASeq_RiboDeplete", true));
        assertFalse(CheckOrMarkCMORequestsTask.isCmoRecipe("RNASeq_RiboDeplete", false));
    }


}
