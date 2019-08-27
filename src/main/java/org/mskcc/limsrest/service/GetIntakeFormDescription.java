package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.security.access.prepost.PreAuthorize;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GetIntakeFormDescription extends LimsTask {
    private static Log log = LogFactory.getLog(GetIntakeFormDescription.class);
    private String type;
    private String request;

    public void init(String type, String request) {
        this.type = type;
        this.request = request;
    }

    @PreAuthorize("hasRole('READ')")
    public Object execute(VeloxConnection conn) {
        LinkedList<List<String>> fieldNames = new LinkedList<>();
        try {
            List<DataRecord> intakeList = new LinkedList<>();
            if (!type.equals("NULL") && !request.equals("NULL")) {
                intakeList = this.dataRecordManager.queryDataRecords("SampleIntakeForm", "SampleType = '" + type + "' and SequencingRequest = '" + request + "'", this.user);
                for (DataRecord r : intakeList) {
                    String[] req = r.getStringVal("RequiredHeaders", user).split(",");
                    for (int i = 0; i < req.length; i++) {
                        fieldNames.add(Arrays.asList(req[i], "Required", "SUCCESS"));
                    }

                    String[] optional = r.getStringVal("OptionalHeaders", user).split(",");
                    for (int i = 0; i < optional.length; i++) {
                        fieldNames.add(Arrays.asList(optional[i], "Optional", "SUCCESS"));
                    }
                }
            } else if (!type.equals("NULL")) {
                intakeList = this.dataRecordManager.queryDataRecords("SampleIntakeForm", "SampleType = '" + type + "'", this.user);
                LinkedList<String> applicable = new LinkedList<>();
                for (DataRecord r : intakeList) {
                    applicable.add(r.getStringVal("SequencingRequest", this.user));
                }
                fieldNames.add(applicable);
            } else if (!request.equals("NULL")) {
                intakeList = this.dataRecordManager.queryDataRecords("SampleIntakeForm", "SequencingRequest = '" + request + "'", this.user);
                LinkedList<String> applicable = new LinkedList<>();
                for (DataRecord r : intakeList) {
                    applicable.add(r.getStringVal("SampleType", this.user));
                }
                fieldNames.add(applicable);
            }
        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info(e.getMessage());
            log.info(sw.toString());
            fieldNames.add(Arrays.asList("", "", e.getMessage()));
        }

        return fieldNames;
    }
}