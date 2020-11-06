package com.fedex.assessment.model;

import java.util.List;
import java.util.Map;

public class Result {
    private Map<String, Float> pricing;
    private Map<String, List<ShipmentType>> shipments;
    private Map<String, TrackStatus> track;

    public Map<String, Float> getPricing() {
        return pricing;
    }

    public void setPricing(Map<String, Float> pricing) {
        this.pricing = pricing;
    }

    public Map<String, List<ShipmentType>> getShipments() {
        return shipments;
    }

    public void setShipments(Map<String, List<ShipmentType>> shipments) {
        this.shipments = shipments;
    }

    public Map<String, TrackStatus> getTrack() {
        return track;
    }

    public void setTrack(Map<String, TrackStatus> track) {
        this.track = track;
    }
}
