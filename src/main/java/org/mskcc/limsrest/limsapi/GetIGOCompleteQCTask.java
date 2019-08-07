package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Returns all IGO Complete QC records for a given sample.
 */
public class GetIGOCompleteQCTask extends LimsTask {
    private static Log log = LogFactory.getLog(GetIGOCompleteQCTask.class);

    protected String sampleId;

    public void init(String sampleId) {
        this.sampleId = sampleId;
    }

    @Override
    public Object execute(VeloxConnection conn) {
        try {
            String whereClause = "PassedQC = 1 AND SeqQCStatus = 'Passed' AND OtherSampleId = '" + sampleId + "'";
            List<DataRecord> limsRequestList = dataRecordManager.queryDataRecords("SeqAnalysisSampleQC", whereClause, user);
            List<SampleQcSummary> result = new ArrayList();
            for (DataRecord r: limsRequestList) {
                SampleQcSummary qcSummary = annotateQcSummary(r);
                result.add(qcSummary);
            }
            return result;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}