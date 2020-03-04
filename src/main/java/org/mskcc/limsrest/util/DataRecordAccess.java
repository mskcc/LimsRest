package org.mskcc.limsrest.util;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.service.requesttracker.RequestTracker;

import java.rmi.RemoteException;

public class DataRecordAccess {
    private static Log log = LogFactory.getLog(RequestTracker.class);

    /**
     * Safely retrieves a String value from a dataRecord
     *
     * @param record
     * @param key
     * @param user
     * @return
     */
    public static String getRecordStringValue(DataRecord record, String key, User user){
        try {
            return record.getStringVal(key, user);
        } catch (NotFound | RemoteException e){
            log.error(String.format("Failed to get key %s from Data Record: %d", key, record.getRecordId()));
        }
        return "";
    }

    /**
     * Safely retrieves a Long Value from a dataRecord
     *
     * @param record
     * @param key
     * @param user
     * @return
     */
    public static Long getRecordLongValue(DataRecord record, String key, User user){
        try {
            return record.getLongVal(key, user);
        } catch (NotFound | RemoteException | NullPointerException e){
            log.error(String.format("Failed to get key %s from Sample Record: %d", key, record.getRecordId()));
        }
        return null;
    }
}
