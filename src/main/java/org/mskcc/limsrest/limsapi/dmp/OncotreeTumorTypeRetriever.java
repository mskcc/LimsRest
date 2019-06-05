package org.mskcc.limsrest.limsapi.dmp;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class OncotreeTumorTypeRetriever implements TumorTypeRetriever {
    private final String tumorTypeServiceUrl;
    private RestTemplate restTemplate = new RestTemplate();

    public OncotreeTumorTypeRetriever(String tumorTypeServiceUrl) {
        this.tumorTypeServiceUrl = tumorTypeServiceUrl;
    }

    @Override
    public List<TumorType> retrieve() {
        ResponseEntity<List<TumorType>> responseEntity = restTemplate.exchange(tumorTypeServiceUrl, HttpMethod.GET,
                null, new ParameterizedTypeReference<List<TumorType>>() {
                });

        return responseEntity.getBody();
    }
}