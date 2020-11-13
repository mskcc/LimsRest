package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.servermanager.PickListConfig;
import com.velox.api.servermanager.PickListManager;
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
    private static final String assayDataType = "ddPCRAssayDatabase";

    public GetDdpcrAssaysTask() {

    }

    public void init() {

    }

    @PreAuthorize("hasRole('READ')")
    @Override
    public List<DdpcrAssay> execute(VeloxConnection conn) {
        User user = conn.getUser();
        List<DataRecord> records;
        try {
            records = conn.getDataRecordManager().queryDataRecords(assayDataType, null, user);
        } catch (IoError | RemoteException | NotFound e) {
            e.printStackTrace();
            log.error(String.format("Failed to query DataRecords."));
            return new ArrayList<>();
        }

        List<DdpcrAssay> assays = new ArrayList<>();
        List<String> assayNames = new ArrayList<>();

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
            assayNames.add(a.assayName);
            assays.add(a);
        }
//        Keep assay picklist n'synch with assay datatype
//        To be removed when picklist can be safely removed (sample submission still needs it as of 11/2020)
        try {
            PickListManager picklister = conn.getDataMgmtServer().getPickListManager(user);
            PickListConfig pickConfig = picklister.getPickListConfig("ddPCR Assay");
            pickConfig.setEntryList(assayNames);
            picklister.storePickListConfig(user, pickConfig);

        } catch (Exception e) {
            e.printStackTrace();
            log.error(String.format("Failed to create assay picklist."));
        }

        return assays;
    }


}
