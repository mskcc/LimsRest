package org.mskcc.limsrest.limsapi.dmp;

import org.apache.log4j.Logger;
import org.mskcc.limsrest.staticstrings.Constants;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class WebServiceDMPSamplesRetriever implements DMPSamplesRetriever {
    private static final Logger LOGGER = Logger.getLogger(WebServiceDMPSamplesRetriever.class);

    private final String restServiceUrl;

    public WebServiceDMPSamplesRetriever(String restServiceUrl) {
        this.restServiceUrl = restServiceUrl;
    }

    @Override
    public List<String> retrieveTrackingIds(LocalDate date) {
        String formattedDate = date.format(Constants.US_DATE_TIME_FORMATTER);

        CMOTrackingIdList cmoTrackingIdList = new RestTemplate().getForObject(getTrackingIdListQuery(formattedDate),
                CMOTrackingIdList.class);

        LOGGER.info(String.format("Response received from DMP for CMO Tracking Id List: %s", cmoTrackingIdList));

        return cmoTrackingIdList.getTrackingIds();
    }

    private String getTrackingIdListQuery(String formattedDate) {
        return String.format("%s/getCMOTrackingIdList" +
                "?date=%s", restServiceUrl, formattedDate);
    }

    @Override
    public List<DMPSample> getDMPSamples(String trackingId) {
        LOGGER.info(String.format("Retrieving CMO Sample Request Details for tracking id: %s", trackingId));
        RestTemplate restTemplate = new RestTemplate();

        CMOSampleRequestDetailsResponse cmoSampleRequestDetailsResponse = restTemplate.getForObject
                (getCMOSampleRequestDetailsQuery(trackingId),
                        CMOSampleRequestDetailsResponse.class);

        List<DMPSample> studies = cmoSampleRequestDetailsResponse.getStudies();

        LOGGER.info(String.format("Retrieved Sample Details for %d studies: %s", studies.size(),
                getStudiesIds(studies)));
        return studies;
    }

    private String getCMOSampleRequestDetailsQuery(String trackingId) {
        return String.format("%s/getCMOSampleRequestDetails?trackingId=%s", restServiceUrl, trackingId);
    }

    private List<String> getStudiesIds(List<DMPSample> studies) {
        return studies.stream()
                .map(s -> s.getStudySampleId())
                .collect(Collectors.toList());
    }
}