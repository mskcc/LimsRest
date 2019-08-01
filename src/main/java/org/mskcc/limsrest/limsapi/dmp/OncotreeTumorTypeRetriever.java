package org.mskcc.limsrest.limsapi.dmp;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class OncotreeTumorTypeRetriever implements TumorTypeRetriever {
    // TODO read from application.properties
    private final String tumorTypeServiceUrl = "http://draco.mskcc.org:9666/tumor_types";
    private RestTemplate restTemplate = new RestTemplate();

    public OncotreeTumorTypeRetriever() {
    }

    @Override
    public List<TumorType> retrieve() {
        ResponseEntity<List<TumorType>> responseEntity = restTemplate.exchange(tumorTypeServiceUrl, HttpMethod.GET,
                null, new ParameterizedTypeReference<List<TumorType>>() {
                });

        return responseEntity.getBody();
    }
}