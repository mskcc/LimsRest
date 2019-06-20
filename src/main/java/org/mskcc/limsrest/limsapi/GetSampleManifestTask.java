package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class GetSampleManifestTask extends LimsTask {
    private Log log = LogFactory.getLog(GetSampleManifestTask.class);

    protected String requestId;

    public void init(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public Object execute(VeloxConnection conn) {
        try {
            log.info("Querying LIMS for request: " + requestId);
            List<DataRecord> request = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
            if (request.size() != 1) {
                log.error("Invalid request Id");
                return "Invalid Request Id";
            }
            List<SampleManifest> smList = new ArrayList<>();
            DataRecord[] childSamples = request.get(0).getChildrenOfType("Sample", user);
            for (int i = 0; i < childSamples.length; i++) {
                DataRecord[] cmoInfoArray = childSamples[i].getChildrenOfType("SampleCMOInfoRecords", user);
                DataRecord cmoInfo = cmoInfoArray[0];
                SampleManifest s = new SampleManifest();

                String correctedcmoId = cmoInfo.getStringVal("correctedcmoId", user);
                String sampleId = cmoInfo.getStringVal("sampleId", user);
                s.setCMO_SAMPLE_ID(correctedcmoId + "_IGO_" + sampleId);

                s.setCMO_PATIENT_ID(cmoInfo.getStringVal("cmoPatientId", user));

                String userSampleId = cmoInfo.getStringVal("userSampleId", user);
                s.setINVESTIGATOR_SAMPLE_ID(userSampleId + "_IGO_" + sampleId);

                s.setINVESTIGATOR_PATIENT_ID(cmoInfo.getStringVal("patientId", user));
                s.setONCOTREE_CODE(cmoInfo.getStringVal("tumorType", user));
                s.setSAMPLE_CLASS(cmoInfo.getStringVal("tumorornormal", user));
                s.setTISSUE_SITE(cmoInfo.getStringVal("tissueLocation", user));
                s.setSAMPLE_TYPE(cmoInfo.getStringVal("sampleOrigin", user));
                s.setSPECIMEN_PRESERVATION_TYPE(cmoInfo.getStringVal("preservation", user));
                s.setSPECIMEN_COLLECTION_YEAR(cmoInfo.getStringVal("collectionYear", user));
                s.setSEX(cmoInfo.getStringVal("gender", user));

                smList.add(s);
            }
            log.info("Returning");
            return smList;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}