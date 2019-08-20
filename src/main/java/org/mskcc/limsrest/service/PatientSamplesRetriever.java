package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import org.mskcc.domain.sample.CorrectedCmoSampleView;

import java.util.List;

public interface PatientSamplesRetriever {
    List<CorrectedCmoSampleView> retrieve(String patientId, DataRecordManager dataRecordManager, User user) throws
            LimsException;
}
