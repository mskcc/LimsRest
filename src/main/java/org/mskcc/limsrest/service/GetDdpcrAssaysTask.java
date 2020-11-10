package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.util.DdpcrAssay;
import org.springframework.security.access.prepost.PreAuthorize;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetDdpcrAssaysTask extends LimsTask {
    private static Log log = LogFactory.getLog(GetDdpcrAssaysTask.class);


    public GetDdpcrAssaysTask() {

    }

    public void init() {

    }

    @PreAuthorize("hasRole('READ')")
    @Override
    public List<DdpcrAssay> execute(VeloxConnection conn) {
        User user = conn.getUser();
        String query = "*";
        List<DataRecord> records = new ArrayList<>();
        try {
            records = conn.getDataRecordManager().queryDataRecords("ddPCRAssayDatabase", null, user);
        } catch (IoError | RemoteException | NotFound e) {
            log.error(String.format("Failed to query DataRecords w/ query: %s", query));
            return new ArrayList<>();
        }
        System.out.println(records);
        // Transform assays into a redacted API response
        List<DdpcrAssay> assays = new ArrayList<>();

        for (DataRecord assayRecord : records) {
            Map<String, Object> sampleFields = null;
            try {
                sampleFields = assayRecord.getFields(user);
            } catch (RemoteException e) {
                e.printStackTrace();
                log.error(String.format("Failed to parse Assays."));
                return new ArrayList<>();
            }
            DdpcrAssay a = new DdpcrAssay(sampleFields);

            assays.add(a);
        }

        return assays;
    }
}
