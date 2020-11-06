package com.fedex.assessment.service;

import com.fedex.assessment.service.rest.APIExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@Service
public class PricingService {
    private static final String PRICING_PATH = "/pricing";
    private final APIExecutor apiExecutor;

    public PricingService(@Autowired APIExecutor apiExecutor) {
        this.apiExecutor = apiExecutor;
    }

    @Async
    public Future<Map<String, Float>> getPricing(List<String> pricing) {
        if (CollectionUtils.isEmpty(pricing)) {
            return new AsyncResult<>(null);
        }
        return new AsyncResult<>(apiExecutor.getValue(PRICING_PATH, pricing,
                new ParameterizedTypeReference<HashMap<String, Float>>() {
                }));
    }
}
