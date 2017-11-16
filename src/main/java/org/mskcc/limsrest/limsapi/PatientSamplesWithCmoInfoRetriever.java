package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.domain.sample.Sample;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.CorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.converter.SampleRecordToSampleConverter;
import org.mskcc.util.CommonUtils;
import org.mskcc.util.VeloxConstants;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class PatientSamplesWithCmoInfoRetriever implements PatientSamplesRetriever {
    private final CorrectedCmoIdConverter<Sample> sampleToCorrectedCmoIdConverter;
    private final SampleRecordToSampleConverter sampleRecordToSampleConverter;

    public PatientSamplesWithCmoInfoRetriever(CorrectedCmoIdConverter<Sample> sampleToCorrectedCmoIdConverter,
                                              SampleRecordToSampleConverter sampleRecordToSampleConverter) {
        this.sampleToCorrectedCmoIdConverter = sampleToCorrectedCmoIdConverter;
        this.sampleRecordToSampleConverter = sampleRecordToSampleConverter;
    }

    @Override
    public List<CorrectedCmoSampleView> retrieve(String patientId, DataRecordManager dataRecordManager, User user)
            throws LimsException {
        CommonUtils.requireNonNullNorEmpty(patientId, "Patient id cannot be empty");

        try {
            List<CorrectedCmoSampleView> cmoSampleViews = new ArrayList<>();
            for (DataRecord sampleRecord : getSampleRecords(patientId, dataRecordManager, user))
                cmoSampleViews.add(getCorrectedCmoSampleView(sampleRecord, user));

            return cmoSampleViews;
        } catch (NotFound | RemoteException | IoError e) {
            throw new LimsException(String.format("Unable to retrieve samples for patient: %s. Cause: %s", patientId,
                    e.getMessage()), e);
        }
    }

    private List<DataRecord> getSampleRecords(String patientId, DataRecordManager dataRecordManager, User user)
            throws NotFound, IoError, RemoteException, LimsException {
        List<DataRecord> samples = new ArrayList<>();
        List<DataRecord> sampleInfoRecords = dataRecordManager.queryDataRecords(VeloxConstants
                .SAMPLE_CMO_INFO_RECORDS, "PatientId = '" + patientId + "'", user);

        for (DataRecord sampleInfoRecord : sampleInfoRecords) {
            List<DataRecord> parentSamples = sampleInfoRecord.getParentsOfType(VeloxConstants.SAMPLE, user);

            if (parentSamples.size() == 0)
                throw new LimsException(String.format("No parent sample found for cmo info record for sample: %s for " +
                                "patient: %s",
                        sampleInfoRecord.getStringVal(VeloxConstants.SAMPLE_ID, user), patientId));

            samples.add(parentSamples.get(0));
        }

        return samples;
    }

    private CorrectedCmoSampleView getCorrectedCmoSampleView(DataRecord sampleRecord, User user) throws LimsException {
        String sampleId = "";
        try {
            sampleId = sampleRecord.getStringVal(Sample.SAMPLE_ID, user);
            Sample sample = sampleRecordToSampleConverter.convert(sampleRecord, user);
            return sampleToCorrectedCmoIdConverter.convert(sample);
        } catch (Exception e) {
            throw new LimsException(String.format("Sample with id: %s error: %s", sampleId, e.getMessage()), e);
        }
    }
}
