package org.mskcc.limsrest.limsapi.dmp;

import java.time.LocalDate;
import java.util.List;

public interface DMPSamplesRetriever {
    List<String> retrieveTrackingIds(LocalDate date);

    List<Study> getStudies(String trackingId);
}
