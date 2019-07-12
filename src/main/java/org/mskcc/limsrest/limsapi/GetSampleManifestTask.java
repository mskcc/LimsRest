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

    protected String [] igoIds;

    public void init(String [] igoIds) {
        this.igoIds = igoIds;
    }

    @Override
    public Object execute(VeloxConnection conn) {
        try {
            List<SampleManifest> smList = new ArrayList<>();

            for (String igoId : igoIds) {
                List<DataRecord> samples = dataRecordManager.queryDataRecords("samplecmoinforecords", "SampleId = '" + igoId +  "'", user);

                DataRecord cmoInfo = samples.get(0);
                SampleManifest s = new SampleManifest();

                String correctedcmoId = cmoInfo.getStringVal("correctedcmoId", user); // TODO
                String sampleId = cmoInfo.getStringVal("sampleId", user);
                s.setIGO_ID(sampleId);

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
            }

            return smList;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}