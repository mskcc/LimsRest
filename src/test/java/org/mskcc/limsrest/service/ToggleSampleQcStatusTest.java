package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import org.junit.jupiter.api.Test;
import org.mskcc.limsrest.MockDataRecord;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;

import static org.junit.Assert.assertEquals;

public class ToggleSampleQcStatusTest {

    @Test
    public void setSeqAnalysisSampleQcStatusPassed() throws Exception {
        DataRecord seqQc = new DataRecord(0L, null,"QC", new MockDataRecord());
        QcStatus complete = QcStatus.PASSED;

        ToggleSampleQcStatusTask.setSeqAnalysisSampleQcStatus(seqQc, complete, complete.getText(), null);

        assertEquals("Passed", seqQc.getDataField("SeqQCStatus", null));
        assertEquals(Boolean.FALSE, seqQc.getDataField("PassedQc", null));
    }

    @Test
    public void setSeqAnalysisSampleQcStatusIGOComplete() throws Exception {
        DataRecord seqQc = new DataRecord(0L,  null,"QC", new MockDataRecord());
        QcStatus complete = QcStatus.IGO_COMPLETE;

        ToggleSampleQcStatusTask.setSeqAnalysisSampleQcStatus(seqQc, complete, complete.getText(), null);

        assertEquals("Passed", seqQc.getDataField("SeqQCStatus", null));
        assertEquals(Boolean.TRUE, seqQc.getDataField("PassedQc", null));
    }
}