package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.HashMap;
import java.util.List;


public class GetAttachmentFileTask extends LimsTask {
    private static Log log = LogFactory.getLog(GetAttachmentFileTask.class);
    protected String recordId;

    public GetAttachmentFileTask() {
    }

    public GetAttachmentFileTask(String recordId) {
        this.recordId = recordId;
    }

    public void init(final String recordId) {
        this.recordId = recordId;

    }

    @PreAuthorize("hasRole('READ')")
    @Override
    public HashMap<String, Object> execute(VeloxConnection conn) {
        HashMap<String, Object> file = new HashMap<>();

        try {
            List<DataRecord> matched = dataRecordManager.queryDataRecords("Attachment", "RecordId =" + recordId, user);
            String fileName = (String) matched.get(0).getDataField("FilePath", user);
            byte[] data = matched.get(0).getAttachmentData(user);
            file.put("fileName", fileName);
            file.put("data", data);
            return file;

        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }


    }


}