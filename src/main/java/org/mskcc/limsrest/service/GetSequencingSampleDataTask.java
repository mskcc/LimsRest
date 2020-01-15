package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.analytics.SequencingSampleData;
import org.mskcc.limsrest.service.sampletracker.WESSampleData;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class GetSequencingSampleDataTask {

    private Log log = LogFactory.getLog(GetWESSampleDataTask.class);
    private String timestamp;
    private ConnectionLIMS conn;
    private User user;

    public GetSequencingSampleDataTask(String timestamp, ConnectionLIMS conn) {
        this.timestamp = timestamp;
        this.conn = conn;
    }


    public List<SequencingSampleData> execute() {
        long start = System.currentTimeMillis();
        try {
            VeloxConnection vConn = conn.getConnection();
            user = vConn.getUser();
            DataRecordManager dataRecordManager = vConn.getDataRecordManager();

            log.info(" Starting GetSequencingSampleDataTask task using timestamp " + timestamp);
            List<DataRecord> sequenncingSampleRecords = new ArrayList<>();
            if (StringUtils.isBlank(timestamp)){
                sequenncingSampleRecords = dataRecordManager.queryDataRecords("SeqAnalysisSampleQC", null, user);
            }else{
                sequenncingSampleRecords = dataRecordManager.queryDataRecords("SeqAnalysisSampleQC", "DateCreated >= " + Long.parseLong(timestamp), user);
            }

            log.info("Num dmpTracker Records: " + sequenncingSampleRecords.size());
        }catch (Throwable e){
            log.error(e.getMessage(), e);
        }
        return null;
    }


    public DataRecord getFirstSampleUnderRequest(DataRecord seqQcRec) throws IoError, RemoteException {
        DataRecord sampleRec = null;
        Stack <DataRecord> sampleStack = new Stack<>();
        List<DataRecord> parentSamples = seqQcRec.getParentsOfType("Sample", user);
        if (parentSamples.size() > 0){
            sampleStack.addAll(parentSamples);
        }
        while (sampleStack.size() > 0){
            DataRecord nextSample = sampleStack.pop();
            if (nextSample.getParentsOfType("Request", user).size()>0){
                return nextSample;
            }
            else if(nextSample.getParentsOfType("Sample", user).size()>0){
                sampleStack.push(nextSample.getParentsOfType("Request", user).get(0));
            }
        }
        return sampleRec;
    }

    public DataRecord getParentRequest(DataRecord sample) throws IoError, RemoteException {
        List<DataRecord> requests = sample.getParentsOfType("Request", user);
        if (requests.size() > 0){
            return requests.get(0);
        }
        return null;
    }

    public SequencingSampleData createSequencingSampleData(DataRecord seqQcRec) throws IoError, RemoteException {
        DataRecord startSample = getFirstSampleUnderRequest(seqQcRec);
        DataRecord request = getParentRequest(startSample);

        return null;
    }

}

