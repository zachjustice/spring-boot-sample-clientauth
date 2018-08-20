package ca.oakey.samples.web.clientauth;


import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAutoConfiguration
public class Config {

    @Bean
    public LoggingRequestInterceptor loggingRequestInterceptor() {
        return new LoggingRequestInterceptor();
    }

    @Bean
    @Autowired
    public RestTemplate restTemplate(
            LoggingRequestInterceptor loggingRequestInterceptor,
            SSLContextFactory sslContextFactory,
            RestTemplateBuilder builder
    ) throws Exception {

        SSLConnectionSocketFactory sslConnectionSocketFactory =
                new SSLConnectionSocketFactory(sslContextFactory.createSSLContext(), new NoopHostnameVerifier());
        HttpClient client = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();

        return builder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
                .additionalInterceptors(loggingRequestInterceptor)
                .build();
    }

}

