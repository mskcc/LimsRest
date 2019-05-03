package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import org.junit.Test;
import org.mskcc.limsrest.MockDataRecord;
import org.mskcc.limsrest.limsapi.assignedprocess.QcStatus;

import static org.junit.Assert.*;

public class ToggleSampleQcStatusTest {

    @Test
    public void setSeqAnalysisSampleQcStatusPassed() throws Exception {
        DataRecord seqQc = new DataRecord(0, "QC", new MockDataRecord());
        QcStatus complete = QcStatus.PASSED;

        ToggleSampleQcStatus.setSeqAnalysisSampleQcStatus(seqQc, complete, complete.getText(), null);

        assertEquals("Passed", seqQc.getDataField("SeqQCStatus", null));
        assertEquals(Boolean.FALSE, seqQc.getDataField("PassedQc", null));
    }

    @Test
    public void setSeqAnalysisSampleQcStatusIGOComplete() throws Exception {
        DataRecord seqQc = new DataRecord(0, "QC", new MockDataRecord());
        QcStatus complete = QcStatus.IGO_COMPLETE;

        ToggleSampleQcStatus.setSeqAnalysisSampleQcStatus(seqQc, complete, complete.getText(), null);

        assertEquals("Passed", seqQc.getDataField("SeqQCStatus", null));
        assertEquals(Boolean.TRUE, seqQc.getDataField("PassedQc", null));
    }
}