package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class GetSequencingRequestsTaskTest {
    @Test
    public void isIMPACTOrHEMEPACT() {
        GetSequencingRequestsTask task = new GetSequencingRequestsTask(0L, Boolean.TRUE, Boolean.TRUE);
        VeloxConnection conn = Mockito.mock(VeloxConnection.class);
        User user = Mockito.mock(User.class);
        DataRecordManager dataRecordManager = Mockito.spy(DataRecordManager.class);
        Mockito.doReturn(user).when(conn).getUser();
        Mockito.doReturn(dataRecordManager).when(conn).getDataRecordManager();

        try {
            when(dataRecordManager.queryDataRecords(
                    Mockito.anyString(),
                    Mockito.anyString(),
                    user
            )).thenReturn(new ArrayList<>());
        } catch (Exception e) {}

        task.execute(conn);

        try {
            Mockito.verify(dataRecordManager, Mockito.times(5)).queryDataRecords(
                    Mockito.anyString(),
                    Mockito.anyString(),
                    user
            );
        } catch (Exception e) {}
    }
}
