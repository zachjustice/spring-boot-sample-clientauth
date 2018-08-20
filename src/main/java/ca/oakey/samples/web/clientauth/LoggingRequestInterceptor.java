package ca.oakey.samples.web.clientauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.*;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ca.oakey.samples.web.clientauth.JsonLogbackEncoder.getLoggingJsonFactory;

/**
 * Created by comdev on 4/29/16.
 */
public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor, AsyncClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingRequestInterceptor.class);

    public LoggingRequestInterceptor() {
    }

    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        long startTime = System.currentTimeMillis();
        traceRequest(request, body);
        try {
            ClientHttpResponse response = execution.execute(request, body);
            traceResponse(response, startTime, request.getURI());
            return response;
        } catch (RuntimeException | IOException ex) {
            traceResponse(ex, startTime, request.getURI());
            throw ex;
        }
    }

    @Override
    public ListenableFuture<ClientHttpResponse> intercept(HttpRequest request,
                                                          byte[] body,
                                                          AsyncClientHttpRequestExecution execution)
            throws IOException {
        final long startTime = System.currentTimeMillis();
        traceRequest(request, body);
        ListenableFuture<ClientHttpResponse> responseFt = execution.executeAsync(request, body);
        responseFt.addCallback(
            (ClientHttpResponse response) -> {
                try {
                    traceResponse(response, startTime, request.getURI());
                } catch (IOException ex) {
                    logger.warn("Error while logging response: ", ex);
                    // log as a warning and ignore.
                }
            }, (Throwable ex) -> {
                try {
                    traceResponse(ex, startTime, request.getURI());
                } catch (IOException ioex) {
                    logger.warn("Error while logging response: ", ioex);
                    // log as a warning and ignore.
                }
            });
        return responseFt;
    }

    private void traceRequest(HttpRequest request, byte[] body) throws IOException {
        try {
            Map<String, Object> requestMap = getTraceRequestMap(request, body);
            logger.info("Trace Request {}", requestMap);
        } catch (Exception ex) {
            logger.error("Error while logging request:", ex);
        }
    }

    protected Map<String, Object> getTraceRequestMap(HttpRequest request, byte[] body) throws IOException {

        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("RequestToURI", request.getURI());
        infoMap.put("Method", request.getMethod());
        if (request.getHeaders() != null && request.getHeaders().getContentType() != null
                && request.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)) {
            infoMap.put("RequestBody", getLoggingJsonFactory().createParser(body).readValueAsTree());
        } else {
            infoMap.put("RequestBody", new String(body, "UTF-8"));
        }
        infoMap.put("Headers", request.getHeaders());


        return infoMap;
    }

    private void traceResponse(ClientHttpResponse response, long startTime, URI requestUri) throws IOException {
        try {
            Map<String, Object> responseMap = getTraceResponseMap(response, startTime, requestUri);
            logger.info("Trace Response {}", responseMap);
        } catch (Exception ex) {
            logger.error("Error while logging response.", ex);
        }
    }

    private void traceResponse(Throwable ex, long startTime, URI requestUri) throws IOException {
        try {
            Map<String, Object> responseMap = getTraceResponseMap(ex, startTime, requestUri);
            logger.info("Trace Response", responseMap);
        } catch (Exception e) {
            logger.error("Error while logging response.", e);
        }
    }

    protected Map<String, Object> getTraceResponseMap(ClientHttpResponse response,
                                                      long startTime,
                                                      URI requestUri)
            throws IOException {
        Map<String, Object> infoMap = new HashMap<>();
        String responseBody = "";

        try {
            responseBody = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }

        infoMap.put("RequestFromURI", requestUri);
        infoMap.put("ResponseCode", response.getStatusCode());
        infoMap.put("StatusText", response.getStatusText());
        if (response.getHeaders() != null && response.getHeaders().getContentType() != null
                && response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)) {
            infoMap.put("ResponseBody", getLoggingJsonFactory().createParser(responseBody)
                    .readValueAsTree());
        } else {
            infoMap.put("ResponseBody", responseBody);
        }
        infoMap.put("Headers", response.getHeaders());
        infoMap.put("TimeTakenInMilliSeconds", (System.currentTimeMillis() - startTime));

        return infoMap;
    }

    protected Map<String, Object> getTraceResponseMap(Throwable ex, long startTime, URI requestUri) throws IOException {
        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("RequestFromURI", requestUri);
        infoMap.put("TimeTakenInMilliSeconds", (System.currentTimeMillis() - startTime));
        infoMap.put("Exception", getExceptionTrace(ex));
        return infoMap;
    }

    protected Map<String, Object> getExceptionTrace(Throwable ex) {
        Map<String, Object> exMap = new HashMap<>();
        exMap.put("message", ex.getMessage());
        exMap.put("stack", Stream.of(ex.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.toList()));
        if (ex.getCause() != null) {
            exMap.put("cause", getExceptionTrace(ex.getCause()));
        }
        return exMap;
    }

}
