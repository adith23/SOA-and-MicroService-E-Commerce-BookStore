package com.globalbooks.adapter.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OrdersRestClient {

    private final RestTemplate restTemplate;
    private final SecurityClient securityClient;

    @Value("${globalbooks.orders.api-uri}")
    private String ordersApiUri;

    public OrdersRestClient(RestTemplate restTemplate, SecurityClient securityClient) {
        this.restTemplate = restTemplate;
        this.securityClient = securityClient;
    }

    public Map<String, Object> createOrder(Map<String, Object> orderPayload) {
        String token = securityClient.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderPayload, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(ordersApiUri, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        
        throw new RuntimeException("Failed to create order via REST API: " + response.getStatusCode());
    }
}
