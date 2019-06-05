package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.domain.sample.Sample;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.CorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.converter.SampleRecordToSampleConverter;
import org.mskcc.util.CommonUtils;
import org.mskcc.util.VeloxConstants;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * PatientSamplesWithCmoInfoRetriever retrieves all samples for given patient from LIMS
 */
public class PatientSamplesWithCmoInfoRetriever implements PatientSamplesRetriever {
    private final static Log LOGGER = LogFactory.getLog(PatientSamplesWithCmoInfoRetriever.class);

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

        LOGGER.info(String.format("Retrieving samples for patient: %s needed to resolve CMO Sample Id counter",
                patientId));

        try {
            List<CorrectedCmoSampleView> cmoSampleViews = new ArrayList<>();
            StringBuilder error = new StringBuilder();
            for (DataRecord sampleRecord : getSampleRecords(patientId, dataRecordManager, user)) {
                try {
                    cmoSampleViews.add(getCorrectedCmoSampleView(sampleRecord, user));
                } catch (Exception e) {
                    error.append(e.getMessage()).append(", ");
                }
            }

            if (error.length() > 0)
                throw new RuntimeException(error.toString());

            LOGGER.info(String.format("Found %d samples for patient %s: %s", cmoSampleViews.size(), patientId,
                    cmoSampleViews));
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
                .SAMPLE_CMO_INFO_RECORDS, "CmoPatientId = '" + patientId + "'", user);

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

            LOGGER.info(String.format("Found sample %s. Retrieving information needed to generate CMO Sample Id",
                    sampleId));

            Sample sample = sampleRecordToSampleConverter.convert(sampleRecord, user);
            return sampleToCorrectedCmoIdConverter.convert(sample);
        } catch (Exception e) {
            throw new LimsException(e.getMessage(), e);
        }
    }
}
