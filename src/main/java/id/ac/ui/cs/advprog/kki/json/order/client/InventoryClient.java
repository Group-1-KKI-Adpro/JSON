package id.ac.ui.cs.advprog.kki.json.order.client;

import id.ac.ui.cs.advprog.kki.json.order.dto.CatalogItemSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class InventoryClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String BASE_URL = "http://localhost:8080/api/catalog";

    public CatalogItemSnapshot getItem(int catalogId) {
        try {
            return restTemplate.getForObject(BASE_URL + "/" + catalogId, CatalogItemSnapshot.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch catalog item " + catalogId);
        }
    }

    public void reserveItem(int catalogId, int quantity) {
        String url = BASE_URL + "/" + catalogId + "/reserve";

        Map<String, Object> body = new HashMap<>();
        body.put("quantity", quantity);

        try {
            restTemplate.postForEntity(url, body, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reserve stock for item " + catalogId);
        }
    }
}