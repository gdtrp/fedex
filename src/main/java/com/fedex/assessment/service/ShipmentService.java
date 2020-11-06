package com.fedex.assessment.service;

import com.fedex.assessment.model.ShipmentType;
import com.fedex.assessment.service.rest.ExternalServiceExecutor;
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

    private final static String SHIPMENT_PATH = "/shipments";
    @Autowired private ExternalServiceExecutor restExecutorService;
    @Async
    public Future<Map<String, List<ShipmentType>>> getShipments(List<String> shipment){
        if (CollectionUtils.isEmpty(shipment)){
            return new AsyncResult<>(null);
        }
        return new AsyncResult<>(restExecutorService.getValue(SHIPMENT_PATH, shipment,
                new ParameterizedTypeReference<HashMap<String, List<ShipmentType>>>() {}));
    }
}