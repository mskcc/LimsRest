package org.mskcc.limsrest.service;

import com.velox.api.datarecord.*;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.util.Messages;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SetOrCreateInteropData extends LimsTask {

    List<Map<String, Object>> data;
    private static Log log = LogFactory.getLog(SetOrCreateInteropData.class);

    public void init(List<Map<String, Object>> data) {
        this.data = data;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public Object execute(VeloxConnection conn) {
        try {
            log.info("interops to be inserted: " + data.size());
            List<Long> recordIds = data.stream().map(this::addOrSetSingleDataRecord)
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());
            dataRecordManager.storeAndCommit(user.getUsername() + " added " + recordIds.size() + " interop records", user);
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
            List<DataRecord> matchedInterop = dataRecordManager.queryDataRecords("InterOpsDatum",
                    "i_Run = '" + run + "' and i_Read = '" + read + "' and i_Lane = '" + lane + "'", user);
            DataRecord interopRecord = matchedInterop.size() < 1 ?
                    dataRecordManager.addDataRecord("InterOpsDatum", user) : matchedInterop.get(0);
            interopRecord.setFields(fields, user);
            return Optional.of(interopRecord.getRecordId());
        } catch (Exception e) {
            log.error(e.getMessage());
            return Optional.empty();
        }
    }
}
