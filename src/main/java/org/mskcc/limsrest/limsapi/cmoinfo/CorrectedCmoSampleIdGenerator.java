package org.mskcc.limsrest.limsapi.cmoinfo;

import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import org.mskcc.domain.CorrectedCmoSampleView;

public interface CorrectedCmoSampleIdGenerator {
    String generate(CorrectedCmoSampleView correctedCmoSampleView, String requestId, DataRecordManager
            dataRecordManager, User user);
}
