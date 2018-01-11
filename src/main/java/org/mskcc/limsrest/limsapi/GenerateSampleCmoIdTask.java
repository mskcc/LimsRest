package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.domain.sample.Sample;
import org.mskcc.limsrest.limsapi.cmoinfo.CorrectedCmoSampleIdGenerator;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.CorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.converter.SampleRecordToSampleConverter;
import org.mskcc.util.VeloxConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenerateSampleCmoIdTask extends LimsTask {
    private final static Log log = LogFactory.getLog(GenerateSampleCmoIdTask.class);

    private final CorrectedCmoSampleIdGenerator correctedCmoSampleIdGenerator;
    private final CorrectedCmoIdConverter<Sample> sampleToCorrectedCmoIdConverter;
    private final SampleRecordToSampleConverter sampleRecordToSampleConverter;

    private String sampleIgoId;

    @Autowired
    public GenerateSampleCmoIdTask(CorrectedCmoSampleIdGenerator correctedCmoSampleIdGenerator,
                                   CorrectedCmoIdConverter<Sample> sampleToCorrectedCmoIdConverter,
                                   SampleRecordToSampleConverter sampleRecordToSampleConverter) {
        this.correctedCmoSampleIdGenerator = correctedCmoSampleIdGenerator;
        this.sampleToCorrectedCmoIdConverter = sampleToCorrectedCmoIdConverter;
        this.sampleRecordToSampleConverter = sampleRecordToSampleConverter;
    }

    public void init(String sampleIgoId) {
        this.sampleIgoId = sampleIgoId;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public String execute(VeloxConnection conn) {
        CorrectedCmoSampleView correctedCmoSampleView = getCorrectedCmoSampleView();

        String cmoId = correctedCmoSampleIdGenerator.generate(correctedCmoSampleView,
                correctedCmoSampleView.getRequestId(), dataRecordManager, user);

        return cmoId;
    }

    private CorrectedCmoSampleView getCorrectedCmoSampleView() {
        try {
            List<DataRecord> sampleRecords = dataRecordManager.queryDataRecords(VeloxConstants.SAMPLE, "SampleId = '"
                    + sampleIgoId + "'", user);

            if (sampleRecords.size() == 0)
                throw new RuntimeException(String.format("No sample found with id: %s", sampleIgoId));
            if (sampleRecords.size() > 1)
                throw new RuntimeException(String.format("Multiple samples found with id: %s", sampleIgoId));

            DataRecord sampleRecord = sampleRecords.get(0);
            Sample sample = sampleRecordToSampleConverter.convert(sampleRecord, user);

            CorrectedCmoSampleView correctedCmoSampleView = sampleToCorrectedCmoIdConverter.convert(sample);

            log.info(String.format("Converted corrected cmo sample view: %s", correctedCmoSampleView));

            return correctedCmoSampleView;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
