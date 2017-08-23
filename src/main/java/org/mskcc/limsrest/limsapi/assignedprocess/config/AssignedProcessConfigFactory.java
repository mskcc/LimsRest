package org.mskcc.limsrest.limsapi.assignedprocess.config;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import org.mskcc.domain.QCStatus;
import org.mskcc.limsrest.limsapi.assignedprocess.repoolsample.RepoolSampleAssignedAssignedProcessConfig;

public class AssignedProcessConfigFactory {
    public AssignedProcessConfig getProcessAssigner(String qcStatusName, DataRecordManager dataRecordManager, DataRecord qc, User user) throws Exception {
        QCStatus qcStatus = QCStatus.getByValue(qcStatusName);

        switch (qcStatus) {
            case RESEQUENCE_POOL:
                return new ResequencePoolAssignedProcessConfig(dataRecordManager, qc, user);
            case REPOOL_SAMPLE:
                return new RepoolSampleAssignedAssignedProcessConfig(user, qc);
            default:
                throw new RuntimeException(String.format("Not supported qc status: %s", qcStatusName));
        }
    }
}
