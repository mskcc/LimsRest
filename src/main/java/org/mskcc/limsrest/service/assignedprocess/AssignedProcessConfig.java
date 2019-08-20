package org.mskcc.limsrest.service.assignedprocess;

import com.velox.api.datarecord.DataRecord;

public interface AssignedProcessConfig {
    DataRecord getSample() throws Exception;

    AssignedProcess getProcessToAssign();
}