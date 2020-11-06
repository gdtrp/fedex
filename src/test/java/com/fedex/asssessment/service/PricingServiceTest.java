package com.fedex.asssessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedex.TestConfig;
import com.fedex.assessment.service.PricingService;
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
public class PricingServiceTest {
    @Autowired
    PricingService pricingService;
    @Autowired
    RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testSuccessfulExecution() throws URISyntaxException, IOException, ExecutionException, InterruptedException {

        Map<String,Float> pricing = new HashMap<>();
        pricing.put("NL", 14.242090605778f);
        pricing.put("CN", 20.503467806384f);
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/pricing?q=NL,CN")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pricing.json")))
                );

        List<String> request = Arrays.asList("NL", "CN");
        Map<String, Float> result = pricingService.getPricing(request).get();
        for (String requestItem : request) {
            assertEquals(result.get(requestItem), pricing.get(requestItem));
        }
        mockServer.verify();
    }


    @Test
    public void testErrorResponse() throws URISyntaxException, ExecutionException, InterruptedException {

        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/pricing?q=NL")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("internal error")
                );
        List<String> request = Arrays.asList("NL");
        Map<String, Float> result = pricingService.getPricing(request).get();
        assertNotNull(result);
        assertNull(result.get("NL"));
        mockServer.verify();
    }

    @Test
    public void testNullRequest() throws ExecutionException, InterruptedException {
        assertNull(pricingService.getPricing(null).get());
    }

    @Test
    public void testEmptyRequest() throws ExecutionException, InterruptedException {
        assertNull(pricingService.getPricing(new ArrayList<>()).get());
    }

}
