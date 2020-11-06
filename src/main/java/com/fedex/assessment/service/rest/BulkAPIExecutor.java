package com.fedex.assessment.service.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AsyncAPIExecutor implements APIExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncAPIExecutor.class);
    private static final int BULK_QUEUE_CAPACITY = 1000;
    private final APIExecutor restExecutorService;
    private final ExecutorService service;
    private final ExecutorService bulkExecutorService = Executors.newCachedThreadPool();
    private final Map<String, BlockingQueue<BulkRequest>> globalParams = new ConcurrentHashMap<>();
    @Value("${service.bulk.flushInterval:5}")
    private int flushInterval;
    @Value("${service.bulk.size:5}")
    private int bulkSize;
    private boolean started = true;


    public AsyncAPIExecutor(@Autowired APIExecutor restExecutorService,
                            @Autowired ExecutorService service) {
        this.restExecutorService = restExecutorService;
        this.service = service;
    }

    @PreDestroy
    private void shutdown() {
        started = false;
    }

    private <P> void init(String path, ParameterizedTypeReference<HashMap<String, P>> responseType) {
        BlockingQueue<BulkRequest> prev = globalParams.putIfAbsent(path, new ArrayBlockingQueue<>(BULK_QUEUE_CAPACITY));
        if (prev == null) {
            bulkExecutorService.execute(new BulkExecutor<>(path, responseType));
        }
    }

    public <P> Map<String, P> getValue(String path, List<String> params, ParameterizedTypeReference<HashMap<String, P>> responseType) {
        init(path, responseType);
        BulkRequest request = new BulkRequest(params);
        globalParams.get(path).add(request);
        try {
            request.latch.await();
            Map<String, P> result = new HashMap<>();
            request.singleRequests.forEach(x -> result.put(x.request, (P) x.response));
            return result;
        } catch (InterruptedException e) {
            Map<String, P> result = new HashMap<>();
            params.forEach(x -> result.put(x, null));
            Thread.currentThread().interrupt();
            return result;
        }
    }

    private class BulkRequest {
        private final CountDownLatch latch;
        private final List<SingleRequest> singleRequests;
        private final AtomicInteger executed = new AtomicInteger(0);

        private BulkRequest(List<String> requests) {
            this.latch = new CountDownLatch(1);
            this.singleRequests = requests.stream().map(x -> new SingleRequest(this, x)).collect(Collectors.toList());
        }
    }

    private class SingleRequest {
        private final String request;
        private final BulkRequest bulkRequest;
        private Object response;

        private SingleRequest(BulkRequest bulkRequest, String request) {
            this.request = request;
            this.bulkRequest = bulkRequest;
        }

        private void setResponse(Object response) {
            this.response = response;
            if (this.bulkRequest.executed.incrementAndGet() == this.bulkRequest.singleRequests.size()) {
                this.bulkRequest.latch.countDown();
            }

        }
    }

    private class BulkExecutor<P> implements Runnable {
        private final String path;
        private final ParameterizedTypeReference<HashMap<String, P>> responseType;

        private BulkExecutor(String path, ParameterizedTypeReference<HashMap<String, P>> responseType) {
            this.path = path;
            this.responseType = responseType;
        }

        @Override
        public void run() {
            List<SingleRequest> params = new ArrayList<>();
            while (started) {
                try {
                    params.addAll(extractParams());
                    boolean readyToExecute = flushInterval == 0 ? params.size() >= bulkSize : params.size() >= bulkSize || !params.isEmpty();
                    if (readyToExecute) {
                        final List<SingleRequest> clone = new ArrayList<>(params);
                        service.execute(() -> {
                            Map<String, P> result = restExecutorService.getValue(path, clone.stream().map(x -> x.request).collect(Collectors.toList()), responseType);
                            logger.info("path {} got result : {}", path, result);
                            clone.forEach(x -> x.setResponse(result != null ? result.get(x.request) : null));
                        });
                        params = new ArrayList<>(bulkSize);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    started = false;
                }
            }
        }

        private List<SingleRequest> extractParams() throws InterruptedException {
            BlockingQueue<BulkRequest> queue = globalParams.get(path);
            if (flushInterval > 0) {
                BulkRequest element = queue.poll(flushInterval, TimeUnit.SECONDS);
                if (element == null) {
                    return new ArrayList<>();
                }
                return element.singleRequests;
            } else {
                return queue.take().singleRequests;
            }
        }
    }
}
