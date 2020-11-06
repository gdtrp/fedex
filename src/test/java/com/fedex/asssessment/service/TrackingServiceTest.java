package com.fedex.asssessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedex.TestConfig;
import com.fedex.assessment.model.TrackStatus;
import com.fedex.assessment.service.TrackService;
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

import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource(properties = {
        "service.host=http://localhost:8080",
})
public class TrackingServiceTest {
    @Autowired
    TrackService trackService;
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

        HashMap<String, TrackStatus> tracking = new HashMap<>();
        tracking.put("109347263", TrackStatus.NEW);
        tracking.put("123456891", TrackStatus.IN_TRANSIT);
        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/track?q=109347263,123456891")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("track.json")))
                );

        List<String> request = Arrays.asList("109347263", "123456891");
        Map<String, TrackStatus> result = trackService.getTrack(request).get();
        for (String requestItem : request) {
            assertEquals(result.get(requestItem), tracking.get(requestItem));
        }
        mockServer.verify();
    }
    @Test
    public void testErrorResponse() throws URISyntaxException, ExecutionException, InterruptedException {

        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("http://localhost:8080/track?q=109347263")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("internal error")
                );
        List<String> request = Arrays.asList("109347263");
        Map<String, TrackStatus> result = trackService.getTrack(request).get();
        assertNotNull(result);
        assertTrue(result.containsKey("109347263"));
        assertNull(result.get("109347263"));
        mockServer.verify();
    }

    @Test
    public void testNullRequest() throws ExecutionException, InterruptedException {
        assertNull(trackService.getTrack(null).get());
    }

    @Test
    public void testEmptyRequest() throws ExecutionException, InterruptedException {
        assertNull(trackService.getTrack(new ArrayList<>()).get());
    }

}
