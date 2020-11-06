package com.fedex.asssessment.service.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fedex.TestConfig;
import com.fedex.assessment.model.TrackStatus;
import com.fedex.assessment.service.TrackService;
import com.fedex.assessment.service.rest.ExternalServiceExecutorImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource(properties = {
        "service.host=http://localhost:8080",
})
public class RestExecutorServiceImplTest {
    @Autowired private ExternalServiceExecutorImpl serviceUtils;

    private MockRestServiceServer mockServer;
    @Autowired
    RestTemplate restTemplate;

    @Before
    public void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }
    @Test
    public void testSuccessfulExecutionWithHost() throws URISyntaxException, JsonProcessingException {
        successfulTestWithHost("http://localhost:8080/test", "http://localhost:8080/test/track?q=109347263,123456891");
    }

    @Test
    public void testSuccessfulExecutionWithHostAndSlash() throws URISyntaxException, JsonProcessingException {
        successfulTestWithHost("http://localhost:8080/test/", "http://localhost:8080/test/track?q=109347263,123456891");
    }
    private void successfulTestWithHost(String host, String expected) throws URISyntaxException, JsonProcessingException {
        String oldValue = (String) ReflectionTestUtils.getField(serviceUtils, "host" );
        Map<String, TrackStatus> tracking = new HashMap<>();
        tracking.put("109347263", TrackStatus.NEW);
        tracking.put("123456891", TrackStatus.IN_TRANSIT);
        try{
            ReflectionTestUtils.setField(serviceUtils, "host", host);
            mockServer.expect(ExpectedCount.once(),
                    requestTo(new URI(expected)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.OK)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"109347263\": \"NEW\", \"123456891\": \"IN TRANSIT\"}")
                    );

            List<String> request = Arrays.asList("109347263", "123456891");
            Map<String, TrackStatus> result = serviceUtils.getValue(TrackService.TRACKING_PATH, request, new ParameterizedTypeReference<HashMap<String, TrackStatus>>() {});
            for (String requestItem : request) {
                assertEquals(result.get(requestItem), tracking.get(requestItem));
            }
            mockServer.verify();
        }finally{
            ReflectionTestUtils.setField(serviceUtils, "host", oldValue);
        }
    }
}
