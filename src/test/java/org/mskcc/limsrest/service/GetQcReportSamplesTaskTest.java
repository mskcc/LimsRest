package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import org.junit.Before;
import org.junit.Test;
import org.mskcc.limsrest.MockDataRecord;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GetQcReportSamplesTaskTest {
    GetQcReportSamplesTask getQcReportSamplesTask;


    @Before
    public void setupSetOrCreateBanked() {
        getQcReportSamplesTask = new GetQcReportSamplesTask();
    }


    @Test
    public void whenRequestNotInDnaQcTable_shouldReturnEmptySampleList() throws Exception {
        getQcReportSamplesTask.otherSampleIds.add("sample1");
        String dataType = "QcReportDna";
        List<QcReportSampleList.ReportSample> reportSamples = getQcReportSamplesTask.getQcSamples(dataType);

        assertTrue(reportSamples.isEmpty());

    }

}
