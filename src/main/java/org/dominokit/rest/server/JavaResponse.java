package org.dominokit.rest.server;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import org.dominokit.rest.shared.Response;

import java.util.Map;
import java.util.stream.Collectors;

public class JavaResponse implements Response {

    private final HttpResponse<Buffer> response;

    JavaResponse(HttpResponse<Buffer> response) {
        this.response = response;
    }

    @Override
    public String getHeader(String header) {
        return response.getHeader(header);
    }

    @Override
    public Map<String, String> getHeaders() {
        return response.headers().entries().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public int getStatusCode() {
        return response.statusCode();
    }

    @Override
    public String getStatusText() {
        return response.statusMessage();
    }

    @Override
    public String getBodyAsString() {
        return response.bodyAsString();
    }
}
