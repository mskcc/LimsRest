
package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * A queued task that finds the barcode sequence for a give barcode id 
 * 
 * @author Aaron Gabow
 */
public class FindBarcodeSequence extends LimsTask {
    private static Log log = LogFactory.getLog(FindBarcodeSequence.class);
    private String barcodeId;

    public void init(String barcodeId) {
        this.barcodeId = barcodeId;
    }

    @PreAuthorize("hasRole('READ')")
    @Override
    public Object execute(VeloxConnection conn) {
        String seq = "";

        try {
            List<DataRecord> indexList = dataRecordManager.queryDataRecords("IndexAssignment", "IndexId = '" + barcodeId + "'", user);
            for (DataRecord i : indexList) {
                seq = i.getStringVal("IndexTag", user);
            }
        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info("ERROR IN SETTING REQUEST STATUS: " + e.getMessage() + "TRACE: " + sw.toString());
        }
        return seq;
    }
}
