package org.mskcc.limsrest.service.dmp;

import java.time.LocalDate;
import java.util.List;

public interface DMPSamplesRetriever {
    List<String> retrieveTrackingIds(LocalDate date);

    List<DMPSample> getDMPSamples(String trackingId);
}
