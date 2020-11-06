package com.fedex.asssessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedex.TestConfig;
import com.fedex.assessment.model.Result;
import com.fedex.assessment.model.ShipmentType;
import com.fedex.assessment.model.TrackStatus;
import com.fedex.assessment.service.AggregationService;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource(properties = {
        "service.host=http://localhost:8080",
})
public class AggregationServiceTest {
    @Autowired
    AggregationService aggregationService;
    @Autowired
    RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(AggregationServiceTest.class);
    @Before
    public void init() {
            mockServer = MockRestServiceServer.createServer(restTemplate);
        }
    @Test
    public void testSuccessfulAggregation() throws IOException, URISyntaxException {
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
        Result result = aggregationService.getAggregation(Arrays.asList("NL", "CN"), Arrays.asList("109347263", "123456891"), Arrays.asList("109347263", "123456891"));
        assertNotNull(result);
        assertNotNull(result.getPricing());
        assertEquals(result.getPricing().size(), 2);
        assertEquals(result.getPricing().get("NL"), 14.242090605778f, 0.0f);
        assertEquals(result.getPricing().get("CN"), 20.503467806384f, 0.0f);
        assertNotNull(result.getShipments());
        assertEquals(result.getShipments().size(), 2);
        assertEquals(result.getShipments().get("109347263").size(), 3);
        assertEquals(result.getShipments().get("109347263").get(0), ShipmentType.box);
        assertEquals(result.getShipments().get("109347263").get(1), ShipmentType.box);
        assertEquals(result.getShipments().get("109347263").get(2), ShipmentType.pallet);
        assertEquals(result.getShipments().get("123456891").size(), 1);
        assertEquals(result.getShipments().get("123456891").get(0), ShipmentType.envelope);
        assertNotNull(result.getTrack());
        assertEquals(result.getTrack().size(), 2);
        assertEquals(result.getTrack().get("109347263"), TrackStatus.NEW);
        assertEquals(result.getTrack().get("123456891"), TrackStatus.IN_TRANSIT);
    }


    @Test
    public void testErrorFromPricing() throws IOException, URISyntaxException {
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
        Result result = aggregationService.getAggregation(Arrays.asList("NL", "CN"), Arrays.asList("109347263", "123456891"), Arrays.asList("109347263", "123456891"));
        assertNotNull(result);

        assertNotNull(result.getPricing());
        assertEquals(result.getPricing().size(), 2);
        assertEquals(result.getPricing().get("NL"), null);
        assertEquals(result.getPricing().get("CN"), null);

        assertNotNull(result.getShipments());
        assertEquals(result.getShipments().size(), 2);
        assertEquals(result.getShipments().get("109347263").size(), 3);
        assertEquals(result.getShipments().get("109347263").get(0), ShipmentType.box);
        assertEquals(result.getShipments().get("109347263").get(1), ShipmentType.box);
        assertEquals(result.getShipments().get("109347263").get(2), ShipmentType.pallet);
        assertEquals(result.getShipments().get("123456891").size(), 1);
        assertEquals(result.getShipments().get("123456891").get(0), ShipmentType.envelope);
        assertNotNull(result.getTrack());
        assertEquals(result.getTrack().size(), 2);
        assertEquals(result.getTrack().get("109347263"), TrackStatus.NEW);
        assertEquals(result.getTrack().get("123456891"), TrackStatus.IN_TRANSIT);

    }

    @Test
    public void testErrorFromShipment() throws IOException, URISyntaxException {
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
        Result result = aggregationService.getAggregation(Arrays.asList("NL", "CN"), Arrays.asList("109347263", "123456891"), Arrays.asList("109347263", "123456891"));
        assertNotNull(result);
        assertNotNull(result.getPricing());
        assertEquals(result.getPricing().size(), 2);
        assertEquals(result.getPricing().get("NL"), 14.242090605778f, 0.0f);
        assertEquals(result.getPricing().get("CN"), 20.503467806384f, 0.0f);
        assertNotNull(result.getShipments());

        assertEquals(result.getShipments().size(), 2);
        assertEquals(result.getShipments().get("109347263"), null);
        assertEquals(result.getShipments().get("123456891"), null);


        assertNotNull(result.getTrack());
        assertEquals(result.getTrack().size(), 2);
        assertEquals(result.getTrack().get("109347263"), TrackStatus.NEW);
        assertEquals(result.getTrack().get("123456891"), TrackStatus.IN_TRANSIT);
    }

    @Test
    public void testErrorFromTracking() throws IOException, URISyntaxException {
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
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("service unavailable")
                );
        Result result = aggregationService.getAggregation(Arrays.asList("NL", "CN"), Arrays.asList("109347263", "123456891"), Arrays.asList("109347263", "123456891"));
        assertNotNull(result);
        assertNotNull(result.getPricing());
        assertEquals(result.getPricing().size(), 2);
        assertEquals(result.getPricing().get("NL"), 14.242090605778f, 0.0f);
        assertEquals(result.getPricing().get("CN"), 20.503467806384f, 0.0f);
        assertNotNull(result.getShipments());
        assertEquals(result.getShipments().size(), 2);
        assertEquals(result.getShipments().get("109347263").size(), 3);
        assertEquals(result.getShipments().get("109347263").get(0), ShipmentType.box);
        assertEquals(result.getShipments().get("109347263").get(1), ShipmentType.box);
        assertEquals(result.getShipments().get("109347263").get(2), ShipmentType.pallet);
        assertEquals(result.getShipments().get("123456891").size(), 1);
        assertEquals(result.getShipments().get("123456891").get(0), ShipmentType.envelope);
        assertNotNull(result.getTrack());
        assertEquals(result.getTrack().get("109347263"), null);
        assertEquals(result.getTrack().get("123456891"), null);
    }

}
