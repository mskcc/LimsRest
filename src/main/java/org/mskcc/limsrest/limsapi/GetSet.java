package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class GetSet extends LimsTask {
    private Log log = LogFactory.getLog(GetSet.class);
    protected String name;

    public void init(String name) {
        this.name = name;
    }

    @PreAuthorize("hasRole('READ')")
    public Object execute(VeloxConnection conn) {
        LinkedList<String> sampleNames = new LinkedList<String>();
        try {
            List<DataRecord> setList = this.dataRecordManager.queryDataRecords("SampleSet", "Name = '" + name + "'", this.user);
            for (DataRecord r : setList) {
                DataRecord[] samples = r.getChildrenOfType("Sample", user);
                DataRecord[] requests = r.getChildrenOfType("Request", user);
                for (int i = 0; i < samples.length; i++) {
                    String correctedSampleName = "";
                    try {
                        correctedSampleName = samples[i].getStringVal("OtherSampleId", user);
                    } catch (NullPointerException npe) {
                    }
                    DataRecord[] infos = samples[i].getChildrenOfType("SampleCMOInfoRecords", user);
                    if (infos.length > 0 && infos[0].getStringVal("CorrectedCMOID", user) != null && !infos[0].getStringVal("CorrectedCMOID", user).equals("")) {
                        correctedSampleName = infos[0].getStringVal("CorrectedCMOID", user);
                    }
                    sampleNames.add(correctedSampleName);
                }
                for (int i = 0; i < requests.length; i++) {
                    DataRecord[] childSamples = requests[i].getChildrenOfType("Sample", user);
                    for (int j = 0; j < childSamples.length; j++) {
                        String correctedSampleName = "";
                        try {
                            correctedSampleName = childSamples[j].getStringVal("OtherSampleId", user);
                        } catch (NullPointerException npe) {
                        }
                        DataRecord[] infos = samples[j].getChildrenOfType("SampleCMOInfoRecords", user);
                        if (infos.length > 0 && infos[0].getStringVal("CorrectedCMOID", user) != null && !infos[0].getStringVal("CorrectedCMOID", user).equals("")) {
                            correctedSampleName = infos[0].getStringVal("CorrectedCMOID", user);
                        }
                        sampleNames.add(correctedSampleName);
                    }
                    DataRecord[] childPlates = requests[i].getChildrenOfType("Plate", user);
                    for (int j = 0; j < childPlates.length; j++) {
                        DataRecord[] plateSamples = childPlates[j].getChildrenOfType("Sample", user);
                        for (int k = 0; k < plateSamples.length; k++) {
                            String correctedSampleName = "";
                            try {
                                plateSamples[k].getStringVal("OtherSampleId", user);
                            } catch (NullPointerException npe) {
                            }
                            DataRecord[] infos = plateSamples[k].getChildrenOfType("SampleCMOInfoRecords", user);
                            if (infos.length > 0 && infos[0].getStringVal("CorrectedCMOID", user) != null && !infos[0].getStringVal("CorrectedCMOID", user).equals("")) {
                                correctedSampleName = infos[0].getStringVal("CorrectedCMOID", user);
                            }
                            sampleNames.add(correctedSampleName);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            sampleNames.add(e.getMessage());
        }

        return sampleNames;
    }
}