package org.mskcc.limsrest.limsapi.dmp;

import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.log4j.Logger;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.limsrest.limsapi.LimsTask;
import org.mskcc.limsrest.limsapi.dmp.converter.DMPToBankedSampleConverter;
import org.mskcc.limsrest.limsapi.store.RecordSaver;
import org.mskcc.limsrest.staticstrings.Messages;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

public class GenerateBankedSamplesFromDMP extends LimsTask {
    private static final Logger LOGGER = Logger.getLogger(GenerateBankedSamplesFromDMP.class);

    private final DMPToBankedSampleConverter dmpToBankedSampleConverter;
    private final DMPSamplesRetriever dmpSamplesRetriever;
    private final RecordSaver recordSaver;
    private LocalDate date;

    public GenerateBankedSamplesFromDMP(DMPToBankedSampleConverter dmpToBankedSampleConverter, DMPSamplesRetriever
            dmpSamplesRetriever, RecordSaver recordSaver) {
        this.dmpToBankedSampleConverter = dmpToBankedSampleConverter;
        this.dmpSamplesRetriever = dmpSamplesRetriever;
        this.recordSaver = recordSaver;
    }

    public void init(LocalDate date) {
        this.date = date;
    }

    @Override
    public ResponseEntity<String> execute(VeloxConnection conn) {
        try {
            List<String> cmoTrackingIds = dmpSamplesRetriever.retrieveTrackingIds(date);

            for (String trackingId : cmoTrackingIds)
                createBankedSamples(trackingId);

            return ResponseEntity.ok(Messages.SUCCESS);
        } catch (Exception e) {
            LOGGER.error(String.format("Unable to retrieve CMO Sample Request Details for date: %s", date), e);
            return ResponseEntity.ok(Messages.ERROR_IN + " retrieving CMO Sample Request Details for date: " +
                    date);
        }
    }

    private void createBankedSamples(String trackingId) {
        List<Study> studies = dmpSamplesRetriever.getStudies(trackingId);

        for (Study study : studies) {
            BankedSample bankedSample = convertToBankedSample(study);
            saveBankedSample(bankedSample);
        }
    }

    private void saveBankedSample(BankedSample bankedSample) {
        recordSaver.save(bankedSample, dataRecordManager, user);
    }

    private BankedSample convertToBankedSample(Study study) {
        return dmpToBankedSampleConverter.convert(study);
    }

}
