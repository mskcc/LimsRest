package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.DataRecord;

public interface AssignedProcessConfig {
    DataRecord getSample() throws Exception;

    AssignedProcess getProcessToAssign();
}