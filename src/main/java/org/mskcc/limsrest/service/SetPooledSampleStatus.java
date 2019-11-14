package org.mskcc.limsrest.service;

import com.velox.api.datarecord.*;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.service.assignedprocess.QcStatusAwareProcessAssigner;
import org.springframework.security.access.prepost.PreAuthorize;
import java.rmi.RemoteException;
import java.util.List;

import static org.mskcc.util.VeloxConstants.SAMPLE;
import static org.mskcc.util.VeloxConstants.SEQ_ANALYSIS_SAMPLE_QC;

public class SetPooledSampleStatus extends LimsTask {
    private static Log log = LogFactory.getLog(ToggleSampleQcStatus.class);

    private long recordId;
    private String status;

    private QcStatusAwareProcessAssigner qcStatusAwareProcessAssigner = new QcStatusAwareProcessAssigner();

    public void init(long recordId, String status) {
        this.recordId = recordId;
        this.status = status;
    }

    /**
     * Uses recordId to find the correct sample to set the status of
     *
     * @param conn
     *
     * @return Boolean, Did value setting succeed
     */
    @PreAuthorize("hasRole('USER')")
    @Override
    public Object execute(VeloxConnection conn) {
        log.info("Searching for record: " + recordId);
        DataRecord record = querySystemForRecord();
        if( record == null ) return Boolean.FALSE;

        DataRecord[] childSamples = getParentsOfType(record, SAMPLE);
        if(childSamples.length == 0) return Boolean.FALSE;

        log.info(String.format("Found record %s. Searching for child sample with 'PoolingSampleLibProtocol'",recordId));
        while(childSamples.length > 0){
            record = childSamples[0];
            if(isRecordForRepooling(record)){
                String pooledSampleRecord = Long.toString(record.getRecordId());
                log.info(String.format("Found sample, %s, with 'PoolingSampleLibProtocol'", pooledSampleRecord));
                setDataField(record, "ExemplarSampleStatus", this.status);
                return Boolean.TRUE;
            }
            childSamples = getChildrenOfType(record, SAMPLE);
        }
        log.info(String.format("No child sample for %s with 'PoolingSampleLibProtocol'", recordId));
        return Boolean.FALSE;
    }

    /**
     * Determines if the Sample DataRecord is the one that should have its status set. This is determiend by whether
     * record has a child type with a 'PoolingSampleLibProtocol' set
     *
     * @param record
     * @return boolean
     */
    private boolean isRecordForRepooling(DataRecord record){
        // TOOD - use com.velox.sloan.cmo.recmodels's code generator
        DataRecord[] poolingSampleLibProtocol = getChildrenOfType(record, "PoolingSampleLibProtocol");
        return poolingSampleLibProtocol.length > 0;
    }

    /**
     * Sets DataRecord's field to input value
     * @param record
     * @param dataFieldName
     * @param newValue
     *
     * @return Boolean, Did value setting succeed
     */
    private Boolean setDataField(DataRecord record, String dataFieldName, String newValue){
        try{
            Object oldStatus = record.getDataField("ExemplarSampleStatus", user);
            log.info(String.format("Setting status of record %s from '%s' to '%s'", record.getRecordId(), oldStatus, this.status));
            record.setDataField("ExemplarSampleStatus", this.status, user);
            dataRecordManager.storeAndCommit("PostSeqAnalysisQC updated to " + status, user);
            return Boolean.TRUE;
        } catch (InvalidValue e){
            log.error(String.format("%s is an invalid value for %s. Error: %s", newValue, dataFieldName, e.getMessage()));
        } catch(NotFound e){
            log.error(String.format("Failed to find record %s. Error: %s", Long.toString(record.getRecordId()), e.getMessage()));
        } catch(IoError | RemoteException e){
            log.error(String.format("Failed to access record %s. Error: %s", Long.toString(record.getRecordId()), e.getMessage()));
        } catch(com.velox.api.util.ServerException e){
            log.error(e);
        }
        return Boolean.FALSE;
    }

    private DataRecord[] getChildrenOfType(DataRecord record, String table){
        try {
            return record.getChildrenOfType(table, user);
        } catch(IoError | RemoteException e){
            log.error(String.format("Error getting children from %s dataType for record %s. Error: %s",
                    table,
                    record.getRecordId(),
                    e.getMessage()));
        }
        return new DataRecord[0];
    }

    private DataRecord[] getParentsOfType(DataRecord record, String table){
        try {
            List<DataRecord> dataRecords = record.getParentsOfType(table, user);
            DataRecord[] dataRecordsArray = new DataRecord[dataRecords.size()];
            return dataRecords.toArray(dataRecordsArray);
        } catch(IoError | RemoteException e){
            log.error(String.format("Error getting parents from %s dataType for record %s. Error: %s",
                    table,
                    record.getRecordId(),
                    e.getMessage()));
        }
        return new DataRecord[0];
    }

    private DataRecord querySystemForRecord(){
        try {
            return dataRecordManager.querySystemForRecord(recordId, SEQ_ANALYSIS_SAMPLE_QC, user);
        } catch(NotFound e){
            log.error(String.format("Record %s not found. Error: ", recordId, e.getMessage()));
        } catch(IoError | RemoteException e){
            log.error(String.format("Error accessing record %s. Error: ", recordId, e.getMessage()));
        }
        return null;
    }
}
