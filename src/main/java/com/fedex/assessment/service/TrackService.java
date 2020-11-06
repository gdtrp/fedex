package com.fedex.assessment.service;

import com.fedex.assessment.model.TrackStatus;
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
public class TrackService {
    public static final String TRACKING_PATH = "/track";
    private final APIExecutor apiExecutor;

    public TrackService(@Autowired APIExecutor apiExecutor) {
        this.apiExecutor = apiExecutor;
    }

    @Async
    public Future<Map<String, TrackStatus>> getTrack(List<String> tracking) {
        if (CollectionUtils.isEmpty(tracking)) {
            return new AsyncResult<>(null);
        }
        return new AsyncResult<>(apiExecutor.getValue(TRACKING_PATH, tracking,
                new ParameterizedTypeReference<HashMap<String, TrackStatus>>() {
                }));
    }
}
