package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import lombok.NoArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

public class GetAttachmentFileTask {
    private static Log log = LogFactory.getLog(GetAttachmentFileTask.class);
    protected String recordId;
    private ConnectionLIMS conn;

    public GetAttachmentFileTask(String recordId, ConnectionLIMS conn) {
        this.recordId = recordId;
        this.conn = conn;
    }

    @PreAuthorize("hasRole('READ')")
    public HashMap<String, Object> execute() {
        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager dataRecordManager = vConn.getDataRecordManager();
        HashMap<String, Object> file = new HashMap<>();
        try {
            log.info("Searching for RecordId ='" + recordId + "'");
            // search ExemplarSDMSFile table first where QC files are after August 12, 2023
            List<DataRecord> matched = dataRecordManager.queryDataRecords("ExemplarSDMSFile", "RecordId =" + recordId, user);
            if (matched.size() > 0) {
                log.info("Files found: " + matched.size());
                String fileName = (String) matched.get(0).getDataField("FilePath", user);
                InputStream is = matched.get(0).openAttachmentDataInputStream(user);
                byte [] data = is.readAllBytes();
                log.info("Bytes read:" + data.length);
                file.put("fileName", fileName);
                file.put("data", data);
                is.close();
            } else if (matched.size() == 0) {
                matched = dataRecordManager.queryDataRecords("Attachment", "RecordId =" + recordId, user);
                String fileName = (String) matched.get(0).getDataField("FilePath", user);
                byte[] data = matched.get(0).getAttachmentData(user);
                file.put("fileName", fileName);
                file.put("data", data);
            }
            return file;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}