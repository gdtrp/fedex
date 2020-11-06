package com.fedex.assessment.service.rest;

import com.fedex.assessment.service.PricingService;
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


public class ExternalServiceExecutorImpl implements ExternalServiceExecutor {
    private Logger logger = LoggerFactory.getLogger(PricingService.class);
    @Value("${service.host}")
    private String host;
    private final static String PARAM_NAME = "q";
    private final static String DELIMITER = ",";
    @Autowired
    private RestTemplate template;
    public <P> Map<String, P> getValue(String path, List<String> params, ParameterizedTypeReference<HashMap<String, P>> responseType){
        URI uri = UriComponentsBuilder.fromHttpUrl(host).path(path).queryParam(PARAM_NAME, params.stream().collect(Collectors.joining(DELIMITER))).build().toUri();
        RequestEntity request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
        try{
            return template.exchange(request, responseType).getBody();
        }catch(RestClientException e){
            logger.warn("error during execution {}. Cause: {}", uri , e.getLocalizedMessage());
            Map<String , P > result = new HashMap<>();
            params.forEach(x -> result.put(x, null));
            return result;
        }
    }
}