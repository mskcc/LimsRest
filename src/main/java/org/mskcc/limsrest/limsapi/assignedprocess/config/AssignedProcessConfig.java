package org.mskcc.limsrest.limsapi.assignedprocess.config;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import org.mskcc.domain.AssignedProcess;

public interface AssignedProcessConfig {
    DataRecord getSample() throws Exception;

    AssignedProcess getProcessToAssign() throws Exception;
}
