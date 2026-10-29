
package com.chandu.roost.probe;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class HttpProbeClientTest {

    private HttpServer server;
    private HttpProbeClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0
        );

        server.createContext("/ok", exchange -> respond(exchange, 200));
        server.createContext("/redirect", exchange -> respond(exchange, 302));
        server.createContext("/not-found", exchange -> respond(exchange, 404));
        server.createContext("/error", exchange -> respond(exchange, 500));
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(1500);
                respond(exchange, 200);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                exchange.close();
            }
        });
        server.start();

        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        client = new HttpProbeClient();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldTreatSlowResponseAsDown() {
        var result = client.probe(baseUrl + "/slow", 200);

        assertFalse(result.healthy());
        assertNotNull(result.errorMessage());
    }

    @Test
    void shouldReportSuccessfulResponseAsUp() {
        var result = client.probe(baseUrl + "/ok", 2000);

        assertTrue(result.healthy());
        assertEquals(200, result.statusCode());
        assertNull(result.errorMessage());
    }

    @Test
    void shouldTreatRedirectAsUp() {
        var result = client.probe(baseUrl + "/redirect", 2000);

        assertTrue(result.healthy());
        assertEquals(302, result.statusCode());
    }

    @Test
    void shouldTreatNotFoundAsDown() {
        var result = client.probe(baseUrl + "/not-found", 2000);

        assertFalse(result.healthy());
        assertEquals(404, result.statusCode());
        assertEquals("HTTP status 404", result.errorMessage());
    }

    @Test
    void shouldTreatServerErrorAsDown() {
        var result = client.probe(baseUrl + "/error", 2000);

        assertFalse(result.healthy());
        assertEquals(500, result.statusCode());
        assertEquals("HTTP status 500", result.errorMessage());
    }

    @Test
    void shouldTreatConnectionFailureAsDown() {
        var result = client.probe(
                "http://127.0.0.1:1/unreachable", 500
        );

        assertFalse(result.healthy());
        assertNull(result.statusCode());
        assertNotNull(result.errorMessage());
        assertFalse(result.errorMessage().isBlank());
    }

    private static void respond(
            com.sun.net.httpserver.HttpExchange exchange,
            int status
    ) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }
}
