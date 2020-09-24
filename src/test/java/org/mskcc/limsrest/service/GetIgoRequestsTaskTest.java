package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.RequestModel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mskcc.limsrest.ConnectionLIMS;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mskcc.limsrest.util.StatusTrackerConfig.isIgoComplete;

public class GetIgoRequestsTaskTest {
    public final Long DAYS_RANGE = 100l;
    ConnectionLIMS conn;

    @Before
    public void setup() {
        // Connection needed to query the existing tango workflow manager
        this.conn = new ConnectionLIMS("tango.mskcc.org", 1099, "fe74d8e1-c94b-4002-a04c-eb5c492704ba", "test-runner", "password1");
    }

    @After
    public void tearDown() {
        this.conn.close();
    }

    private Map<String, DataRecord> getIgoRequestDataRecords(Boolean getCompleteRequests) {
        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager drm = vConn.getDataRecordManager();

        GetIgoRequestsTask task = new GetIgoRequestsTask(DAYS_RANGE, getCompleteRequests);
        List<RequestSummary> requests = task.execute(vConn);

        Map<String, DataRecord> validatedRecords = new HashMap<>();

        for (RequestSummary request : requests) {
            String requestId = request.getRequestId();
            List<DataRecord> records = new ArrayList<>();
            try {
                records = drm.queryDataRecords(RequestModel.DATA_TYPE_NAME, String.format("RequestId = '%s'", requestId), user);
            } catch (NotFound e) {
                Assert.assertTrue(String.format("Data Record for request Id: %s doesn't exist. Update test", requestId), false);
            } catch (IoError | RemoteException e) {
                Assert.assertTrue(String.format("Error getting Data Record for request Id: %s", requestId), false);
            }
            if (records.size() != 1) {
                Assert.assertTrue(String.format("Data Record %s is ambiguous or doesn't exist. Update test"), false);
            }

            validatedRecords.put(requestId, records.get(0));
        }

        return validatedRecords;
    }

    @Test
    public void getIgoRequestsTask_matchesIsIgoCompleteUtil_incomplete() {
        Boolean isComplete = Boolean.FALSE;
        Map<String, DataRecord> dataRecords = getIgoRequestDataRecords(isComplete);
        Assert.assertTrue("No data records to test. Increase @DAYS_RANGE", dataRecords.size() > 0);
        for (Map.Entry<String, DataRecord> record : dataRecords.entrySet()) {
            Assert.assertEquals(String.format("Record Id: %s didn't match expected completion status; %b",
                    record.getKey(), isComplete),
                    isComplete,
                    isIgoComplete(record.getValue(), conn.getConnection().getUser()));
        }
    }

    @Test
    public void getIgoRequestsTask_matchesIsIgoCompleteUtil_complete() {
        Boolean isComplete = Boolean.TRUE;
        Map<String, DataRecord> dataRecords = getIgoRequestDataRecords(isComplete);
        Assert.assertTrue("No data records to test. Increase @DAYS_RANGE", dataRecords.size() > 0);
        for (Map.Entry<String, DataRecord> record : dataRecords.entrySet()) {
            Assert.assertEquals(String.format("Record Id: %s didn't match expected completion status; %b",
                    record.getKey(), isComplete),
                    isComplete,
                    isIgoComplete(record.getValue(), conn.getConnection().getUser()));
        }
    }
}

