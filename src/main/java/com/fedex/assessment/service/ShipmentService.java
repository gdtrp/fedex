package com.fedex.assessment.service;

import com.fedex.assessment.model.ShipmentType;
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
public class ShipmentService {
    private static final String SHIPMENT_PATH = "/shipments";
    private final APIExecutor apiExecutor;

    public ShipmentService(@Autowired APIExecutor apiExecutor) {
        this.apiExecutor = apiExecutor;
    }

    @Async
    public Future<Map<String, List<ShipmentType>>> getShipments(List<String> shipment) {
        if (CollectionUtils.isEmpty(shipment)) {
            return new AsyncResult<>(null);
        }
        return new AsyncResult<>(apiExecutor.getValue(SHIPMENT_PATH, shipment,
                new ParameterizedTypeReference<HashMap<String, List<ShipmentType>>>() {
                }));
    }
}
