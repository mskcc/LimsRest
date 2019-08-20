package org.mskcc.limsrest.service.dmp;

import org.springframework.web.client.RestTemplate;

import java.util.Set;

/**
 * Reads the list of Onco Tree tumor types.
 */
public class OncotreeTumorTypeRetriever implements TumorTypeRetriever {
    // TODO read from application.properties
    private final String tumorTypeServiceUrl = "http://draco.mskcc.org:9666/tumor_types";
    private RestTemplate restTemplate = new RestTemplate();

    public OncotreeTumorTypeRetriever() {
    }

    @Override
    public Set<TumorType> retrieve() {
        OncoTreeTumorTypeSet response = restTemplate.getForObject(tumorTypeServiceUrl, OncoTreeTumorTypeSet.class);
        return response.results;
    }

    static class OncoTreeTumorTypeSet {
        public Set<TumorType> results;
    }
}