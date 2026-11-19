package com.tcs.VendorPulse.inventory.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
public class InventoryClient {
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${inventory.api.url:http://localhost:8081/mock-inventory/materials}")
    private String baseUrl;

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetch(String materialCode) {
        return restTemplate.getForObject(baseUrl + "/" + materialCode, Map.class);
    }
}
