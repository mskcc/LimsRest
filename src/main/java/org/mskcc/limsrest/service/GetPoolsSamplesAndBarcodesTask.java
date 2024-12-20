package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;

import java.rmi.RemoteException;
import java.util.*;

/**
 * A queued task that takes a pool ID and returns the parent library sample IGO ID and the assigned barcode information.
 * Like index ID, index tag
 *
 * @author Fahimeh Mirhaj
 */

public class GetPoolsSamplesAndBarcodesTask {

    private static Log log = LogFactory.getLog(GetReadyForIllumina.class);
    private ConnectionLIMS conn;
    private String poolId;
    public GetPoolsSamplesAndBarcodesTask(ConnectionLIMS conn, String poolId) {
        this.conn = conn;
        this.poolId = poolId;
    }
    public List<PoolInfo> execute() {
        List<PoolInfo> result = new LinkedList<>();
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager dataRecordManager = vConn.getDataRecordManager();
            DataRecord pool = dataRecordManager.queryDataRecords("Sample", "SampleId ='" + poolId + "'", user).get(0);
            List<String> parentLibrarySamplesForPool = getNearestParentLibrarySamplesForPool(pool, user);

            for (String eachLibSample : parentLibrarySamplesForPool) {
                BarcodeSummary barcodes = null;
                List<DataRecord> indexList = new LinkedList<>();
                Set<String> hierarchyOfSamples = new HashSet<>();
                hierarchyOfSamples.add(poolId);
                String[] sampleIdParts = eachLibSample.split("_");
                for (int i = 0; i < sampleIdParts.length; i++) {
                    eachLibSample = "";
                    for (int j = 0; j < sampleIdParts.length - i; j++) {
                        if (j > 0) {
                            eachLibSample += "_";
                        }
                        eachLibSample += sampleIdParts[j];
                        log.info("Lib sample aliquot: " + eachLibSample);
                        hierarchyOfSamples.add(eachLibSample);
                    }
                }
                String sampleWithBarcodeAssigned = "";
                for (String eachSample : hierarchyOfSamples) {
                    if (indexList.size() > 0) {
                        break;
                    }
                    log.info("Looking for barcode assigned to " + eachSample);
                    indexList = dataRecordManager.queryDataRecords("IndexBarcode", "SampleId = '" + eachSample + "' ORDER BY IndexId", user);
                    if (!indexList.isEmpty()) {
                        sampleWithBarcodeAssigned = eachSample;
                    }
                }

                log.info("sample igo id with barcode assigned:" + sampleWithBarcodeAssigned);
                for (DataRecord i : indexList) {
                    try {
                        log.info("indexId: " + i.getStringVal("IndexId", user));
                        barcodes = new BarcodeSummary(i.getStringVal("IndexId", user), i.getStringVal("IndexTag", user));
                    } catch (NullPointerException npe) {
                        log.error("Null pointer exception at instantiating Barcode Summary!");
                    }
                }
                result.add(new PoolInfo(sampleWithBarcodeAssigned, barcodes));
            }

        } catch (Exception e) {
            log.error("An exception occurred while running GetPoolsSamplesAndBarcodesTask with message: " + e.getMessage());
        }
        return result;
    }


    /**
     * If the sample is pool, get all the samples of type Library that are present in the pool.
     * @param pooledSample
     * @return List of Library samples in the pooled sample.
     * @throws IoError
     * @throws RemoteException
     * @throws NotFound
     */
    private List<String> getNearestParentLibrarySamplesForPool(DataRecord pooledSample, User user) throws IoError, RemoteException, NotFound , ServerException {
        List<String> parentLibrarySamplesForPool = new ArrayList<>();
        Stack<DataRecord> sampleTrackingStack = new Stack<>();
        sampleTrackingStack.add(pooledSample);
        while (!sampleTrackingStack.isEmpty()) {
            List<DataRecord> parentSamples = sampleTrackingStack.pop().getParentsOfType("Sample", user);
            if (!parentSamples.isEmpty()) {
                for (DataRecord sample : parentSamples) {
                    String sampleId = sample.getStringVal("SampleId", user);
                    log.info("Processing: " + sampleId);
                    if (sampleId.toLowerCase().startsWith("pool-")) {
                        sampleTrackingStack.push(sample);
                    }
                    else {
                        parentLibrarySamplesForPool.add(sample.getStringVal("SampleId", user));
                    }
                }
            }
        }
        return parentLibrarySamplesForPool;
    }
}
