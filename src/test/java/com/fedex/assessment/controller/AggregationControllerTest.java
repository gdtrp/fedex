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

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@TestPropertySource(properties = {
        "service.host=http://localhost:8080",
        "service.bulk.flushInterval=5",
        "service.bulk.size=5",
})
public class AggregationControllerTest {

    TestRestTemplate testRestTemplate = new TestRestTemplate();

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
    public void testInboundValidation() throws URISyntaxException, IOException {

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://localhost").port(port).path("aggregation");

        URI uri = builder
                .queryParam("pricing", "NLA,CN")
                .build().toUri();

        RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
        ResponseEntity<String> response = testRestTemplate.exchange(request, String.class);
        assertEquals(response.getStatusCodeValue(), 400);




        uri = builder
                .queryParam("track", "1093472631,123456891")
                .build().toUri();

        request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
        response = testRestTemplate.exchange(request, String.class);
        assertEquals(response.getStatusCodeValue(), 400);



        uri = builder
                .queryParam("shipments", "1093472631,1234568911")
                .build().toUri();

        request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
        response = testRestTemplate.exchange(request, String.class);
        assertEquals(response.getStatusCodeValue(), 400);


        uri = builder
                .queryParam("track", "10934726,1234568")
                .build().toUri();

        request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
        response = testRestTemplate.exchange(request, String.class);
        assertEquals(response.getStatusCodeValue(), 400);


        uri = builder
                .queryParam("shipments", "10934726,12345689")
                .build().toUri();

        request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
        response = testRestTemplate.exchange(request, String.class);
        assertEquals(response.getStatusCodeValue(), 400);
    }
    @Test
    public void testSuccessfulExecution() throws URISyntaxException, IOException {
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/pricing?q=NL,CN")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pricing.json")))
                );
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/shipments?q=109347263,123456891")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("shipment.json")))
                );
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/track?q=109347263,123456891")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("track.json")))
                );

        URI uri = UriComponentsBuilder.fromHttpUrl("http://localhost").port(port).path("aggregation")
                .queryParam("pricing", "NL,CN")
                .queryParam("track", "109347263,123456891")
                .queryParam("shipments", "109347263,123456891")
                .build().toUri();

        RequestEntity request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
        ResponseEntity<Map> response = testRestTemplate.exchange(request, Map.class);
        assertEquals(response.getStatusCodeValue(), HttpStatus.OK.value());
        assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);
        Map result = response.getBody();
        assertNotNull(result.get("pricing"));
        assertNotNull(result.get("track"));
        assertNotNull(result.get("shipments"));
    }



    @Test
    public void testErrorPricing() throws URISyntaxException, IOException {
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/pricing?q=NL,CN")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("service unavailable")
                );
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/shipments?q=109347263,123456891")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("shipment.json")))
                );
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/track?q=109347263,123456891")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("track.json")))
                );

        URI uri = UriComponentsBuilder.fromHttpUrl("http://localhost").port(port).path("aggregation")
                .queryParam("pricing", "NL,CN")
                .queryParam("track", "109347263,123456891")
                .queryParam("shipments", "109347263,123456891")
                .build().toUri();

        RequestEntity request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
        ResponseEntity<Map> response = testRestTemplate.exchange(request, Map.class);
        assertEquals(response.getStatusCodeValue(), HttpStatus.OK.value());
        assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);
        Map result = response.getBody();
        assertNotNull(result.get("track"));
        assertNotNull(result.get("shipments"));
        assertNotNull(result.get("pricing"));
        assertNull(((Map)result.get("pricing")).get("NL"));
        assertTrue(((Map)result.get("pricing")).containsKey("NL"));
        assertNull(((Map)result.get("pricing")).get("CN"));
        assertTrue(((Map)result.get("pricing")).containsKey("CN"));
    }


    @Test
    public void testErrorShipment() throws URISyntaxException, IOException {
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/pricing?q=NL,CN")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pricing.json")))
                );
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/shipments?q=109347263,123456891")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("service unavailable")
                );
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/track?q=109347263,123456891")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("track.json")))
                );

        URI uri = UriComponentsBuilder.fromHttpUrl("http://localhost").port(port).path("aggregation")
                .queryParam("pricing", "NL,CN")
                .queryParam("track", "109347263,123456891")
                .queryParam("shipments", "109347263,123456891")
                .build().toUri();

        RequestEntity request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
        ResponseEntity<Map> response = testRestTemplate.exchange(request, Map.class);
        assertEquals(response.getStatusCodeValue(), HttpStatus.OK.value());
        assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);
        Map result = response.getBody();
        assertNotNull(result.get("pricing"));
        assertNotNull(result.get("track"));
        assertNotNull(result.get("shipments"));
        assertNull(((Map)result.get("shipments")).get("109347263"));
        assertTrue(((Map)result.get("shipments")).containsKey("109347263"));
        assertNull(((Map)result.get("shipments")).get("123456891"));
        assertTrue(((Map)result.get("shipments")).containsKey("123456891"));
    }


    @Test
    public void testErrorShipments() throws URISyntaxException, IOException {
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/pricing?q=NL,CN")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pricing.json")))
                );
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/shipments?q=109347263,123456891")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("service unavailable")
                );
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/track?q=109347263,123456891")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("track.json")))
                );

        URI uri = UriComponentsBuilder.fromHttpUrl("http://localhost").port(port).path("aggregation")
                .queryParam("pricing", "NL,CN")
                .queryParam("track", "109347263,123456891")
                .queryParam("shipments", "109347263,123456891")
                .build().toUri();

        RequestEntity request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
        ResponseEntity<Map> response = testRestTemplate.exchange(request, Map.class);
        assertEquals(response.getStatusCodeValue(), HttpStatus.OK.value());
        assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON_UTF8);
        Map result = response.getBody();
        assertNotNull(result.get("pricing"));
        assertNotNull(result.get("track"));
        assertNotNull(result.get("shipments"));
        assertTrue(((Map)result.get("shipments")).containsKey("109347263"));
        assertNull(((Map)result.get("shipments")).get("109347263"));
        assertTrue(((Map)result.get("shipments")).containsKey("123456891"));
        assertNull(((Map)result.get("shipments")).get("123456891"));
    }
}
