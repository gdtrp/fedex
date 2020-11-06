package com.fedex.assessment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedex.assessment.Application;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.*;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@TestPropertySource(properties = {
        "service.host=http://localhost:8080",
        "service.bulk.size=5",
        "service.bulk.flushInterval=0"
})
public class AggregationControllerWithoutFlushIntervalTest {

    @Autowired
    RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    @LocalServerPort
    private int port;
    private Logger logger = LoggerFactory.getLogger(AggregationControllerTest.class);
    private ObjectMapper mapper = new ObjectMapper();
    @Before
    public void init() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    }

    @Test
    public void testSuccessfulExecution() throws URISyntaxException, IOException, InterruptedException, ExecutionException, TimeoutException {
        mockServer.expect(ExpectedCount.once(),
                requestTo(startsWith("http://localhost:8080/pricing") ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pricing.json")))
                );
        mockServer.expect(ExpectedCount.once(),
                requestTo(startsWith("http://localhost:8080/shipments")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("shipment.json")))
                );
        mockServer.expect(ExpectedCount.once(),
                requestTo(startsWith("http://localhost:8080/track")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("track.json")))
                );


        ExecutorService executor = Executors.newCachedThreadPool();
        Future<ResponseEntity<Map>> future1 = executor.submit(() -> {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://localhost").port(port).path("aggregation");
            URI uri = builder
                    .queryParam("pricing", "NL,CN")
                    .queryParam("track", "109347263,123456891")
                    .queryParam("shipments", "109347263,123456891")
                    .build().toUri();

            RequestEntity request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
            TestRestTemplate testRestTemplate = new TestRestTemplate();
            ResponseEntity<Map> response = testRestTemplate.exchange(request, Map.class);
            return response;
        });

        Future<ResponseEntity<Map>> future2 = executor.submit(() -> {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://localhost").port(port).path("aggregation");
            URI uri = builder
                    .queryParam("pricing", "DE,AM")
                    .queryParam("track", "123456890,123456893")
                    .queryParam("shipments", "123456890,123456893")
                    .build().toUri();

            RequestEntity request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
            TestRestTemplate testRestTemplate = new TestRestTemplate();
            return testRestTemplate.exchange(request, Map.class);
        });

        Future<ResponseEntity<Map>> future3 = executor.submit(() -> {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://localhost").port(port).path("aggregation");
            URI uri = builder
                    .queryParam("pricing", "GB")
                    .queryParam("track", "123456892")
                    .queryParam("shipments", "123456892")
                    .build().toUri();

            RequestEntity request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
            TestRestTemplate testRestTemplate = new TestRestTemplate();
            return testRestTemplate.exchange(request, Map.class);
        });

        ResponseEntity<Map> response1 = future1.get(3, TimeUnit.SECONDS);
        ResponseEntity<Map> response2 = future2.get(3, TimeUnit.SECONDS);
        ResponseEntity<Map> response3 = future3.get(3, TimeUnit.SECONDS);


        assertEquals(response1.getStatusCodeValue(), HttpStatus.OK.value());
        assertEquals(response1.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);
        Map result = response1.getBody();
        assertNotNull(result.get("pricing"));
        assertNotNull(result.get("track"));
        assertNotNull(result.get("shipments"));



        assertEquals(response2.getStatusCodeValue(), HttpStatus.OK.value());
        assertEquals(response2.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);
        result = response2.getBody();
        assertNotNull(result.get("pricing"));
        assertNotNull(result.get("track"));
        assertNotNull(result.get("shipments"));



        assertEquals(response3.getStatusCodeValue(), HttpStatus.OK.value());
        assertEquals(response3.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);
        result = response3.getBody();
        assertNotNull(result.get("pricing"));
        assertTrue(((Map)result.get("pricing")).containsKey("GB"));
        assertNull(((Map)result.get("pricing")).get("GB"));
        assertNotNull(result.get("track"));
        assertNotNull(result.get("shipments"));
    }
}
