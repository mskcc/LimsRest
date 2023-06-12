package org.mskcc.limsrest.service;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class ArchivedFastqTest {

    @Test
    public void getFlowCellIdTypeA() {
        ArchivedFastq x = new ArchivedFastq();
        x.setRun("JAX_0454_AHHWKVBBXY");
        assertEquals("HHWKVBBXY", x.getFlowCellId());
    }

    @Test
    public void getFlowCellIdTypeB() {
        ArchivedFastq x = new ArchivedFastq();
        x.setRun("TOMS_5394_000000000-J2FY2");
        assertEquals("J2FY2", x.getFlowCellId());
    }

    @Test
    public void getRunId() {
        ArchivedFastq x = new ArchivedFastq();
        x.setRun("TOMS_5394_000000000-J2FY2");
        assertEquals("TOMS_5394", x.getRunId());
    }
}