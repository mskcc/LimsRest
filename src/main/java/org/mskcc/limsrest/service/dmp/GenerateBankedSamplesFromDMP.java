package org.mskcc.limsrest.service.dmp;

import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.limsrest.service.LimsTask;
import org.mskcc.limsrest.service.converter.ExternalToBankedSampleConverter;
import org.mskcc.limsrest.service.dmp.converter.DMPSampleToCMOBankedSampleConverter;
import org.mskcc.limsrest.service.retriever.LimsDataRetriever;
import org.mskcc.limsrest.service.retriever.VeloxLimsDataRetriever;
import org.mskcc.limsrest.service.store.RecordSaver;
import org.mskcc.limsrest.service.store.VeloxRecordSaver;
import org.mskcc.limsrest.util.Messages;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

public class GenerateBankedSamplesFromDMP extends LimsTask {
    private static final Log LOGGER = LogFactory.getLog(GenerateBankedSamplesFromDMP.class);

    private static final String TRACKING_ID_REGEX = "[a-zA-Z0-9_-]+";
    private final ExternalToBankedSampleConverter externalToBankedSampleConverter =
            new DMPSampleToCMOBankedSampleConverter(new OncotreeTumorTypeRetriever());
    //TODO set string normally
    protected DMPSamplesRetriever dmpSamplesRetriever = new WebServiceDMPSamplesRetriever("http://plvpathhydra1.mskcc.org:8001/");
    protected RecordSaver recordSaver = new VeloxRecordSaver();
    protected LimsDataRetriever limsDataRetriever = new VeloxLimsDataRetriever();

    private LocalDate date;

    public GenerateBankedSamplesFromDMP() {
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public String execute(VeloxConnection conn) {
        try {
            LOGGER.info(String.format("Retrieving tracking ids for date: %s", date));
            List<String> cmoTrackingIds = dmpSamplesRetriever.retrieveTrackingIds(date);

            LOGGER.info(String.format("Tracking ids retrieved for date: %s %s", date, cmoTrackingIds));

            long transactionId = LocalDateTime.now().toInstant(ZoneOffset.ofTotalSeconds(0)).getEpochSecond();

            int trackingIdCounter = 0;
            for (String trackingId : cmoTrackingIds) {
                if (shouldProcess(trackingId)) {
                    long newTransactionId = getTransactionId(transactionId, trackingIdCounter);
                    LOGGER.info(String.format("Assigning transaction id: %s for DMP tracking id: %s",
                            newTransactionId, trackingId));

                    createBankedSamples(trackingId, newTransactionId);
                    trackingIdCounter++;
                }
            }

            return Messages.SUCCESS;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns transaction id different for each tracking id. As transaction id is kept in seconds and different
     * tracking ids can be processed in the same second, transaction id has to be modified to be unique.
     *
     * @param transactionId     timestamp in seconds
     * @param trackingIdCounter counter associated with tracking id, different for each tracking id
     * @return
     */
    private long getTransactionId(long transactionId, int trackingIdCounter) {
        return transactionId + trackingIdCounter;
    }

    private boolean shouldProcess(String trackingId) {
        try {
            validateTrackingId(trackingId);

            String query = String.format("%s = '%s'", BankedSample.DMP_TRACKING_ID, trackingId);
            List<BankedSample> bankedSamples = limsDataRetriever.getBankedSamples(query, dataRecordManager, user);

            if (bankedSamples.size() == 0)
                return true;
            else
                LOGGER.info(String.format("DMP tracking id: %s has already been processed and converted to Banked " +
                        "Samples. Banked Samples for this tracking id won't be created", trackingId));
        } catch (Exception e) {
            LOGGER.warn(String.format("Unable to check if tracking id: \"%s\" has already been processed.",
                    trackingId), e);
        }

        return false;
    }

    private void validateTrackingId(String trackingId) {
        if (!trackingId.matches(TRACKING_ID_REGEX)) {
            throw new RuntimeException(String.format("DMP Tracking id: \"%s\" is not in correct format: %s Banked " +
                    "Samples for that DMP Tracking id won't be created.", trackingId, TRACKING_ID_REGEX));
        }
    }

    private void createBankedSamples(String trackingId, long transactionId) {
        LOGGER.info(String.format("Retrieving DMP samples for tracking id: %s", trackingId));
        List<DMPSample> dmpSamples = dmpSamplesRetriever.getDMPSamples(trackingId);

        LOGGER.info(String.format("Retrieved DMP sampled for tracking id: %s %s", trackingId, getStudiesIds
                (dmpSamples)));

        for (DMPSample dmpSample : dmpSamples) {
            BankedSample bankedSample = convertToBankedSample(dmpSample, transactionId);
            saveBankedSample(bankedSample);
        }
    }

    private List<String> getStudiesIds(List<DMPSample> studies) {
        return studies.stream()
                .map(DMPSample::getStudySampleId)
                .collect(Collectors.toList());
    }

    private void saveBankedSample(BankedSample bankedSample) {
        recordSaver.save(bankedSample, dataRecordManager, user);
    }

    private BankedSample convertToBankedSample(DMPSample dmpSample, long transactionId) {
        return externalToBankedSampleConverter.convert(dmpSample, transactionId);
    }
}