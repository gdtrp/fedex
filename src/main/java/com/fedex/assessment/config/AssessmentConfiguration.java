package com.fedex.assessment.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedex.assessment.service.rest.BulkAPIExecutor;
import com.fedex.assessment.service.rest.APIExecutor;
import com.fedex.assessment.service.rest.SyncAPIExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AssessmentConfiguration {
    @Value("${service.bulk.enabled:true}")
    boolean enableBulkService;
    @Value("${service.connectionTimeout:30}")
    int connectionTimeout;
    @Value("${service.readTimeout:30}")
    int readTimeout;

    @Bean
    public ObjectMapper mapper() {
        return new ObjectMapper();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.of(connectionTimeout, ChronoUnit.SECONDS))
                .setReadTimeout(Duration.of(readTimeout, ChronoUnit.SECONDS))
                .build();
    }


    @Bean
    public APIExecutor apiExecutor(@Autowired @Qualifier("restExecutor") APIExecutor restExecutorService, @Autowired ExecutorService service) {
        return enableBulkService ? new BulkAPIExecutor(restExecutorService, service) : restExecutorService;
    }

    @Bean("restExecutor")
    public SyncAPIExecutor restExecutorServiceImpl(@Autowired RestTemplate template) {
        return new SyncAPIExecutor(template);
    }

    @Bean
    public ExecutorService cachedThreadPool() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        return new ConcurrentTaskExecutor(
                Executors.newCachedThreadPool());
    }
}
