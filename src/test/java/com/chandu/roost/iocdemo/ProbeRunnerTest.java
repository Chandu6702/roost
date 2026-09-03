package com.chandu.roost.iocdemo;

import com.chandu.roost.config.ProbeProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProbeRunnerTest {

    @Test
    void shouldUseMockHttpProbeClient() {

        MockHttpProbeClient mockClient = new MockHttpProbeClient();

        ProbeRunner runner = new ProbeRunner(mockClient, new ProbeProperties(60,5000));

        String result = runner.run("https://example.com");

        assertEquals("Mock response", result);
    }

    static class MockHttpProbeClient extends HttpProbeClient {

        @Override
        public String probe(String url) {
            return "Mock response";
        }
    }
}