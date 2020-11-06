package com.fedex.asssessment.service;

import com.fedex.TestConfig;
import com.fedex.assessment.model.ShipmentType;
import com.fedex.assessment.service.ShipmentService;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource(properties = {
        "service.host=http://localhost:8080",
})
public class ShipmentServiceTest {

    @Autowired
    ShipmentService shipmentService;
    @Autowired
    RestTemplate restTemplate;
    private MockRestServiceServer mockServer;


    @Before
    public void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testSuccessfulExecution() throws URISyntaxException, IOException, ExecutionException, InterruptedException {
        Map<String, List<ShipmentType>> shipment = new HashMap<>();
        shipment.put("109347263", Arrays.asList(ShipmentType.box, ShipmentType.box, ShipmentType.pallet));
        shipment.put("123456891", Arrays.asList(ShipmentType.envelope));
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/shipments?q=109347263,123456891")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("shipment.json")))
                );

        List<String> request = Arrays.asList("109347263", "123456891");
        Map<String, List<ShipmentType>> result = shipmentService.getShipments(request).get();
        for (String requestItem : request) {
            assertEquals(result.get(requestItem), shipment.get(requestItem));
        }
        mockServer.verify();
    }


    @Test
    public void testErrorResponse() throws URISyntaxException, ExecutionException, InterruptedException {

        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/shipments?q=109347263")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("internal error")
                );
        List<String> request = Arrays.asList("109347263");
        Map<String, List<ShipmentType>> result = shipmentService.getShipments(request).get();
        assertNotNull(result);
        assertTrue(result.containsKey("109347263"));
        assertNull(result.get("109347263"));
        mockServer.verify();
    }

    @Test
    public void testNullRequest() throws ExecutionException, InterruptedException {
        assertNull(shipmentService.getShipments(null).get());
    }

    @Test
    public void testEmptyRequest() throws ExecutionException, InterruptedException {
        assertNull(shipmentService.getShipments(new ArrayList<>()).get());
    }

}
