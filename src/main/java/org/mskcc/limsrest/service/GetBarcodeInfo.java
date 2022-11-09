package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.LinkedList;
import java.util.List;

/**
 * Returns the list of all LIMS barcodes.
 * 
 * @author Aaron Gabow
 */
public class GetBarcodeInfo {
    private static Log log = LogFactory.getLog(GetBarcodeInfo.class);
    private ConnectionLIMS conn;

    public GetBarcodeInfo(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @PreAuthorize("hasRole('READ')")
    public List<BarcodeSummary> execute() {
        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager dataRecordManager = vConn.getDataRecordManager();

        try {
            List<BarcodeSummary> barcodes = new LinkedList<>();
            List<DataRecord> indexList = dataRecordManager.queryDataRecords("IndexAssignment", "IndexType != 'IDT_TRIM' ORDER BY IndexType, IndexId", user);
            for (DataRecord i : indexList) {
                try {
                    barcodes.add(new BarcodeSummary(i.getStringVal("IndexType", user), i.getStringVal("IndexId", user), i.getStringVal("IndexTag", user)));
                } catch (NullPointerException npe) {
                }
            }
            return barcodes;
        } catch (Throwable e) {
            log.error("ERROR IN SETTING REQUEST STATUS: " + e.getMessage(), e);
            return null;
        }
    }
}