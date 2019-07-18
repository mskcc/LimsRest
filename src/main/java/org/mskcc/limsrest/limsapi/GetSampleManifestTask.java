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
                List<DataRecord> samples = dataRecordManager.queryDataRecords("SampleCMOInfoRecords", "SampleId = '" + igoId +  "'", user);

                DataRecord cmoInfo = samples.get(0);
                String sampleId = cmoInfo.getStringVal("SampleId", user);
                SampleManifest s = new SampleManifest();
                s.setIGO_ID(igoId);

                String cmoPatientId = cmoInfo.getStringVal("CmoPatientId", user);
                s.setCMO_PATIENT_ID(cmoPatientId);
                String userSampleId = cmoInfo.getStringVal("UserSampleID", user);
                s.setINVESTIGATOR_SAMPLE_ID(userSampleId + "_IGO_" + sampleId);

                s.setONCOTREE_CODE(cmoInfo.getStringVal("TumorType", user));
                s.setSAMPLE_CLASS(cmoInfo.getStringVal("TumorOrNormal", user));
                s.setTISSUE_SITE(cmoInfo.getStringVal("TissueLocation", user));
                s.setSAMPLE_TYPE(cmoInfo.getStringVal("SampleOrigin", user));
                s.setSPECIMEN_PRESERVATION(cmoInfo.getStringVal("Preservation", user));
                s.setSPECIMEN_COLLECTION_YEAR(cmoInfo.getStringVal("CollectionYear", user));
                s.setGENDER(cmoInfo.getStringVal("Gender", user));

                List<DataRecord> indexBarcodes = dataRecordManager.queryDataRecords("IndexBarcode", "SampleId = '" + igoId + "'", user);
                if (indexBarcodes != null && indexBarcodes.size() > 0) {
                    // TODO convert to LIST, handle multiple barcodes
                    DataRecord bc = indexBarcodes.get(0);
                    s.setBARCODE_ID(bc.getStringVal("IndexId", user));
                    s.setBARCODE_INDEX(bc.getStringVal("IndexTag", user));
                    if (indexBarcodes.size() > 1)
                        System.err.println("ERROR, need LIST.");
                }
                smList.add(s);
            }

            return smList;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}