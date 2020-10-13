package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class GetSequencingRequestsTaskTest {
    @Test
    public void queriesRequestsWithFlowCellSamples() {
        Integer numDays = 7;
        Boolean delivered = Boolean.TRUE;

        GetSequencingRequestsTask task = new GetSequencingRequestsTask(numDays, delivered);
        VeloxConnection conn = Mockito.mock(VeloxConnection.class);
        User user = Mockito.mock(User.class);
        DataRecord sample = Mockito.mock(DataRecord.class);
        DataRecordManager dataRecordManager = Mockito.spy(DataRecordManager.class);
        Mockito.doReturn(user).when(conn).getUser();
        Mockito.doReturn(dataRecordManager).when(conn).getDataRecordManager();

        try {
            when(dataRecordManager.queryDataRecords(
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.any(User.class)
            )).thenReturn(new ArrayList<>());

            when(dataRecordManager.getParentsOfType(
                    Mockito.anyListOf(DataRecord.class),
                    Mockito.anyString(),
                    Mockito.any(User.class)
            )).thenReturn(Arrays.asList(Arrays.asList(sample)));

            when(sample.getStringVal(
                    Mockito.anyString(),
                    Mockito.any(User.class)
            )).thenReturn("TEST_REQUEST");
        } catch (Exception e) {
            assertTrue(String.format("Failed to mock: %s", e.getMessage()), false);
        }

        task.execute(conn);

        try {
            Mockito.verify(dataRecordManager, Mockito.times(2)).queryDataRecords(
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.any(User.class)
            );
        } catch (Exception e) {
            assertTrue(String.format("Mockito verify failed: %s", e.getMessage()), false);
        }
    }
}
