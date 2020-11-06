package com.fedex.assessment.service.rest;

import org.springframework.core.ParameterizedTypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface APIExecutor {

    <P> Map<String, P> getValue(String path, List<String> params, ParameterizedTypeReference<HashMap<String, P>> responseType);
}
