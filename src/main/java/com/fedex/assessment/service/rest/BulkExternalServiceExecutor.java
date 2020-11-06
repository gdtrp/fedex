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

public class BulkExternalServiceExecutor implements ExternalServiceExecutor {

    private static final  Logger logger = LoggerFactory.getLogger(BulkExternalServiceExecutor.class);


    @Value("${service.bulk.flushInterval:5}") private int flushInterval;
    private static final int BULK_QUEUE_CAPACITY = 1000;

    private Map<String, BlockingQueue<BulkRequest>> globalParams = new ConcurrentHashMap<>();
    private boolean started = true;

    private final ExternalServiceExecutor restExecutorService;
    private final ExecutorService service;
    @Value("${service.bulk.size:5}") private int bulkSize;




    public BulkExternalServiceExecutor(@Autowired ExternalServiceExecutor restExecutorService,
                                       @Autowired ExecutorService service){
        this.restExecutorService = restExecutorService;
        this.service = service;
    }
    @PreDestroy
    private void shutdown(){
        started = false;
    }
    private class BulkRequest{
        private CountDownLatch latch;
        private List<SingleRequest> singleRequests;
        private AtomicInteger executed = new AtomicInteger(0);
        private BulkRequest(List<String> requests){
            this.latch = new CountDownLatch(1);
            this.singleRequests = requests.stream().map(x -> new SingleRequest(this, x)).collect(Collectors.toList());
        }
    }
     private class SingleRequest{
        private String request;
        private Object response;
        private BulkRequest bulkRequest;
        private SingleRequest(BulkRequest bulkRequest, String request){
            this.request = request;
            this.bulkRequest = bulkRequest;
        }
        private void setResponse(Object response){
            this.response = response;
            if (this.bulkRequest.executed.incrementAndGet() == this.bulkRequest.singleRequests.size()){
                this.bulkRequest.latch.countDown();
            }

        }
    }
    public <P> void init(String path, ParameterizedTypeReference<HashMap<String, P>> responseType){
        BlockingQueue<BulkRequest> prev = globalParams.putIfAbsent(path, new ArrayBlockingQueue<>(BULK_QUEUE_CAPACITY));
        if (prev == null){
            service.execute(()-> {
                List<SingleRequest> params = new ArrayList<>();
                while(started){
                    try {
                        boolean readyForExecute;
                        BlockingQueue<BulkRequest> queue = globalParams.get(path);
                        if (flushInterval > 0){
                            BulkRequest element = queue.poll(flushInterval, TimeUnit.SECONDS);
                            if (element != null){
                                params.addAll(element.singleRequests);
                            }
                            readyForExecute = params.size() >= bulkSize || (!params.isEmpty() && element == null);
                        }else{
                            params.addAll(queue.take().singleRequests);
                            readyForExecute = params.size() >=  bulkSize;
                        }
                        if (readyForExecute){
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
            });
        }
    }
    public <P> Map<String, P> getValue(String path, List<String> params, ParameterizedTypeReference<HashMap<String, P>> responseType){
        init(path, responseType);
        BulkRequest request = new BulkRequest(params);
        globalParams.get(path).add(request);
        try {
            request.latch.await();
            Map<String, P> result = new HashMap<>();
            request.singleRequests.forEach(x -> result.put(x.request, (P) x.response));
            return result;
        } catch (InterruptedException e) {
            Map<String , P > result = new HashMap<>();
            params.forEach(x -> result.put(x, null));
            Thread.currentThread().interrupt();
            return result;
        }
    }
}
