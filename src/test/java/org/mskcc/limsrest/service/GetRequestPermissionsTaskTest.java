package org.mskcc.limsrest.service;

import org.junit.Test;

import static org.junit.Assert.*;

public class GetRequestPermissionsTaskTest {

    @Test
    public void labHeadEmailToLabNameSpecialEmail() {
        assertEquals("massaguej", GetRequestPermissionsTask.labHeadEmailToLabName("j-massague@ski.mskcc.org"));
    }

    @Test
    public void labHeadEmailToLabNameNormalEmail() {
        assertEquals("persona", GetRequestPermissionsTask.labHeadEmailToLabName("persona@mskcc.org"));
        assertEquals("kmariens", GetRequestPermissionsTask.labHeadEmailToLabName("kmariens@sloankettering.edu"));
    }

    @Test
    public void labHeadEmailToLabNameBlankEmail() {
        assertEquals("_EXTERNAL", GetRequestPermissionsTask.labHeadEmailToLabName(""));
    }
}