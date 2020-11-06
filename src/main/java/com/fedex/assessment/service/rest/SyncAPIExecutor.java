package com.fedex.assessment.service.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class SyncAPIExecutor implements APIExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SyncAPIExecutor.class);
    private static final String PARAM_NAME = "q";
    private static final String DELIMITER = ",";
    private final RestTemplate template;
    @Value("${service.host}")
    private String host;

    public SyncAPIExecutor(@Autowired RestTemplate template) {
        this.template = template;
    }

    public <P> Map<String, P> getValue(String path, List<String> params, ParameterizedTypeReference<HashMap<String, P>> responseType) {
        URI uri = UriComponentsBuilder.fromHttpUrl(host).path(path).queryParam(PARAM_NAME, params.stream().collect(Collectors.joining(DELIMITER))).build().toUri();
        RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
        try {
            return template.exchange(request, responseType).getBody();
        } catch (RestClientException e) {
            logger.warn("error during execution {}. Cause: {}", uri, e.getLocalizedMessage());
            Map<String, P> result = new HashMap<>();
            params.forEach(x -> result.put(x, null));
            return result;
        }
    }
}
