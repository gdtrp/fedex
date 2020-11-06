package com.fedex.assessment.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TrackStatus{
    NEW,
    @JsonProperty("IN TRANSIT")
    IN_TRANSIT, COLLECTING, COLLECTED, DELIVERING, DELIVERED
}
