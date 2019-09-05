package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;
import org.mskcc.limsrest.service.assignedprocess.QcStatusAwareProcessAssigner;
import org.springframework.security.access.prepost.PreAuthorize;

public class PoolInformationCollector extends LimsTask {
    private static Log log = LogFactory.getLog(ToggleSampleQcStatus.class);

    long recordId;

    private QcStatusAwareProcessAssigner qcStatusAwareProcessAssigner = new QcStatusAwareProcessAssigner();

    public void init(long recordId) {
        this.recordId = recordId;
    }

    @PreAuthorize("hasRole('USER')")
    @Override
    public Object execute(VeloxConnection conn) {
        DataRecord seqQc = null;
        try {
            seqQc = dataRecordManager.querySystemForRecord(recordId, "SeqAnalysisSampleQC", user);
        } catch (Throwable e) {
            System.out.println(e);
        }
        return seqQc;
    }
}
