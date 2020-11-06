package com.fedex.assessment.service;

import com.fedex.assessment.model.TrackStatus;
import com.fedex.assessment.model.Result;
import com.fedex.assessment.model.ShipmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
public class AggregationService {
    @Autowired PricingService pricingService;
    @Autowired ShipmentService shipmentService;
    @Autowired TrackService trackingService;
    private Logger logger = LoggerFactory.getLogger(AggregationService.class);
    public Result getAggregation(List<String> pricing, List<String> tracking, List<String> shipment){
        Result result = new Result();
        Future<Map<String, Float>> pricingFuture = pricingService.getPricing(pricing);
        Future<Map<String, List<ShipmentType>>> shipmentFuture = shipmentService.getShipments(shipment);
        Future<Map<String, TrackStatus>> trackingFuture = trackingService.getTrack(tracking);
        try {
            result.setPricing(pricingFuture.get());
            result.setShipments(shipmentFuture.get());
            result.setTrack(trackingFuture.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            logger.warn("error during execution {}", e);
        }
        return result;
    }
}
