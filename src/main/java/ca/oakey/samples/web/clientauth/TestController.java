package ca.oakey.samples.web.clientauth;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.UUID;

@RestController
public class TestController {
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private RestTemplate restTemplate;

    /*
     * Return the authenticated username and roles.
     */
    @GetMapping("/whoami")
    public String whoami() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        // The usom-correlationid is being set as a header by the interceptor so we don't care what this uuid is.
        httpHeaders.set("UUID", UUID.randomUUID().toString());
        httpHeaders.set("api_key", "7ab4c12e-c064-4a49-9538-e75c59175e66");

        try {
            return restTemplate.exchange(
//                    "https://localhost:8111/server/",
                    "https://thdapi.homedepot.com/cts/api/v1/sku/productCode/search?sku=218340",
                    HttpMethod.GET,
                    new HttpEntity(httpHeaders),
                    String.class
            ).getBody();
        } catch (HttpServerErrorException ex) {
            logger.error(ex.getMessage());
            throw ex;
        }
    }

}