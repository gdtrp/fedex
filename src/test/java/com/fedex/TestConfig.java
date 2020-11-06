package com.fedex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedex.assessment.service.AggregationService;
import com.fedex.assessment.service.PricingService;
import com.fedex.assessment.service.ShipmentService;
import com.fedex.assessment.service.TrackService;
import com.fedex.assessment.service.rest.ExternalServiceExecutorImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ComponentScans
({@ComponentScan("com.fedex.assessment.controller"),  @ComponentScan("com.fedex.assessment.service")})
public class TestConfig {

    @Bean
    public ObjectMapper mapper(){
        return new ObjectMapper();
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    ExternalServiceExecutorImpl restExecutorService(){
        return new ExternalServiceExecutorImpl();
    }
    @Bean
    AggregationService aggregationService(){
        return new AggregationService();
    }
    @Bean
    PricingService pricingService(){
        return new PricingService();
    }
    @Bean
    ShipmentService shipmentService(){
        return new ShipmentService();
    }
    @Bean
    TrackService trackService(){
        return new TrackService();
    }
}
