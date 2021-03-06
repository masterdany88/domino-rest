package org.dominokit.domino.rest.gwt;

import org.dominokit.domino.rest.shared.BaseRestfulRequest;
import org.dominokit.domino.rest.shared.RestfulRequest;
import org.dominokit.domino.rest.shared.request.RequestTimeoutException;
import org.gwtproject.timer.client.Timer;
import org.gwtproject.xhr.client.XMLHttpRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

public class JsRestfulRequest extends BaseRestfulRequest {

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_PDF = "application/pdf";
    private XMLHttpRequest request;
    private Map<String, List<String>> params = new LinkedHashMap<>();
    private Map<String, String> headers = new LinkedHashMap<>();
    private final Timer timer = new Timer() {
        @Override
        public void run() {
            fireOnTimeout();
        }
    };

    public JsRestfulRequest(String uri, String method) {
        super(uri, method);
        request = XMLHttpRequest.create();
        parseUri(uri);
    }

    private void parseUri(String uri) {
        if (uri.contains("?")) {
            String[] uriParts = uri.split("\\?");
            addQueryString(uriParts[1]);
        }
    }

    @Override
    protected String paramsAsString() {
        return params.entrySet().stream()
                .map(this::entryAsString)
                .collect(joining("&"));
    }

    private String entryAsString(Map.Entry<String, List<String>> paramValuePair) {
        return paramValuePair.getValue().stream()
                .map(v -> paramValuePair.getKey() + "=" + v)
                .collect(joining("&"));
    }

    @Override
    public RestfulRequest addQueryParam(String key, String value) {
        if (!params.containsKey(key))
            params.put(key, new ArrayList<>());
        params.get(key).add(value);
        return this;
    }

    @Override
    public RestfulRequest setQueryParam(String key, String value) {
        params.put(key, new ArrayList<>());
        addQueryParam(key, value);
        return this;
    }

    @Override
    public RestfulRequest putHeader(String key, String value) {
        if (CONTENT_TYPE.equalsIgnoreCase(key)) {
            if (APPLICATION_PDF.equalsIgnoreCase(value)) {
                request.setResponseType(XMLHttpRequest.ResponseType.ArrayBuffer);
            }
        }
        headers.put(key, value);
        return this;
    }

    @Override
    public RestfulRequest putHeaders(Map<String, String> headers) {
        if (nonNull(headers)) {
            headers.forEach(this::putHeader);
        }
        return this;
    }

    @Override
    public RestfulRequest putParameters(Map<String, String> parameters) {
        if (nonNull(parameters) && !parameters.isEmpty()) {
            parameters.forEach(this::addQueryParam);
        }
        return this;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public void sendForm(Map<String, String> formData) {
        setContentType("application/x-www-form-urlencoded");
        send(formData.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(joining("&")));
    }

    @Override
    public void sendJson(String json) {
        setContentType("application/json");
        send(json);
    }

    @Override
    public void send(String data) {
        initRequest();
        request.send(data);
    }

    @Override
    public void send() {
        initRequest();
        request.send();
    }

    @Override
    public void abort() {
        request.clearOnReadyStateChange();
        request.abort();
    }

    @Override
    public RestfulRequest setResponseType(String responseType) {
        request.setResponseType(responseType);
        return this;
    }

    private void setContentType(String contentType) {
        headers.put(CONTENT_TYPE, contentType);
    }

    private void initRequest() {
        String url = getUri();
        request.open(getMethod(), url);
        setHeaders();
        request.setOnReadyStateChange(xhr -> {
            if (xhr.getReadyState() == XMLHttpRequest.DONE) {
                xhr.clearOnReadyStateChange();
                timer.cancel();
                successHandler.onResponseReceived(new JsResponse(xhr));
            }
        });
        if (getTimeout() > 0) {
            timer.schedule(getTimeout());
        }
    }

    private void fireOnTimeout() {
        timer.cancel();
        request.clearOnReadyStateChange();
        request.abort();
        errorHandler.onError(new RequestTimeoutException());
    }

    private void setHeaders() {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.setRequestHeader(entry.getKey(), entry.getValue());
        }
    }
}
