package org.mskcc.limsrest.service;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.util.Messages;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SetOrCreateInteropData {
    private static Log log = LogFactory.getLog(SetOrCreateInteropData.class);
    private List<Map<String, Object>> data;
    private User user;
    private DataRecordManager drm;

    public SetOrCreateInteropData(List<Map<String, Object>> data, ConnectionLIMS conn) {
        this.data = data;
        VeloxConnection vConn = conn.getConnection();
        user = vConn.getUser();
        drm = vConn.getDataRecordManager();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String execute() {
        try {
            log.info("interops to be inserted: " + data.size());
            List<Long> recordIds = data.stream().map(this::addOrSetSingleDataRecord)
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());
            drm.storeAndCommit(user.getUsername() + " added " + recordIds.size() + " interop records", user);
            log.info("interops inserted: " + recordIds.size());
            return StringUtils.join(recordIds, ",");
        } catch (Exception e) {
            log.error(e);
            return Messages.ERROR_IN + " SETTING INTEROPSDATUM: " + e.getMessage();
        }
    }

    private Optional<Long> addOrSetSingleDataRecord(Map<String, Object> fields) {
        try {
            String run = String.valueOf(fields.get("i_Run"));
            String read = String.valueOf(fields.get("i_Read"));
            String lane = String.valueOf(fields.get("i_Lane"));
            List<DataRecord> matchedInterop = drm.queryDataRecords("InterOpsDatum",
                    "i_Run = '" + run + "' and i_Read = '" + read + "' and i_Lane = '" + lane + "'", user);
            DataRecord interopRecord = matchedInterop.size() < 1 ?
                    drm.addDataRecord("InterOpsDatum", user) : matchedInterop.get(0);
            interopRecord.setFields(fields, user);
            return Optional.of(interopRecord.getRecordId());
        } catch (Exception e) {
            log.error(e.getMessage());
            return Optional.empty();
        }
    }
}
