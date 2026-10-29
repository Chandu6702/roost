
package com.chandu.roost.probe;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class HttpProbeClient {

    public ProbeOutcome probe(String url, Integer timeoutMillis) {
        try {
            Duration timeout = Duration.ofMillis(timeoutMillis);

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(timeout)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

            JdkClientHttpRequestFactory requestFactory =
                    new JdkClientHttpRequestFactory(httpClient);

            requestFactory.setReadTimeout(timeout);

            RestClient restClient = RestClient.builder()
                    .requestFactory(requestFactory)
                    .build();

            return restClient.get()
                    .uri(url)
                    .exchange((request, response) -> {
                        int statusCode = response.getStatusCode().value();

                        if (statusCode >= 200 && statusCode < 400) {
                            return new ProbeOutcome(
                                    true,
                                    statusCode,
                                    null
                            );
                        }

                        return new ProbeOutcome(
                                false,
                                statusCode,
                                "HTTP status " + statusCode
                        );
                    });

        } catch (Exception exception) {
            String reason = exception.getMessage();

            if (reason == null || reason.isBlank()) {
                reason = exception.getClass().getSimpleName();
            }

            return new ProbeOutcome(false, null, reason);
        }
    }

    public record ProbeOutcome(
            boolean healthy,
            Integer statusCode,
            String errorMessage
    ) {
    }
}
