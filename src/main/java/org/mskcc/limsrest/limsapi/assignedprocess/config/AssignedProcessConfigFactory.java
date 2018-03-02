package org.mskcc.limsrest.limsapi.assignedprocess.config;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import org.mskcc.domain.QcStatus;
import org.mskcc.limsrest.limsapi.assignedprocess.repoolsample.RepoolSampleAssignedAssignedProcessConfig;
import org.mskcc.limsrest.limsapi.assignedprocess.resequencepool.ResequencePoolAssignedProcessConfig;

public class AssignedProcessConfigFactory {
    public AssignedProcessConfig getProcessAssignerConfig(String qcStatusName, DataRecordManager dataRecordManager, DataRecord qc, User user) throws Exception {
        QcStatus qcStatus = QcStatus.getByValue(qcStatusName);

        switch (qcStatus) {
            case RESEQUENCE_POOL:
                return new ResequencePoolAssignedProcessConfig(qc, user);
            case REPOOL_SAMPLE:
                return new RepoolSampleAssignedAssignedProcessConfig(user, qc);
            default:
                throw new RuntimeException(String.format("Not supported qc status: %s", qcStatusName));
        }
    }
}
