
package com.chandu.roost.probe;

import com.chandu.roost.config.ProbeProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProbeRunnerTest {

    @Test
    void shouldUseMockHttpProbeClient() {

        MockHttpProbeClient mockClient = new MockHttpProbeClient();

        ProbeRunner runner = new ProbeRunner(
                mockClient,
                new ProbeProperties(60, 5000)
        );

        HttpProbeClient.ProbeOutcome result =
                runner.run("https://example.com", 5000);

        assertEquals(true, result.healthy());
        assertEquals(200, result.statusCode());
        assertEquals(null, result.errorMessage());
    }

    static class MockHttpProbeClient extends HttpProbeClient {

        @Override
        public ProbeOutcome probe(String url, Integer timeoutMillis) {
            return new ProbeOutcome(true, 200, null);
        }
    }
}
