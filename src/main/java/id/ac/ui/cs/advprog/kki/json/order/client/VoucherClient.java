package id.ac.ui.cs.advprog.kki.json.order.client;

import id.ac.ui.cs.advprog.kki.json.voucher.dto.ValidateVoucherResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class VoucherClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String BASE_URL = "http://localhost:8080/api/vouchers";

    public double applyVoucher(String code, double total) {
        String url = BASE_URL + "/validate";

        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("orderTotal", total);

        ValidateVoucherResponse response =
                restTemplate.postForObject(url, body, ValidateVoucherResponse.class);

        return response.getFinalTotal();
    }

    public void useVoucher(String code, String orderId, String token) {
        String url = BASE_URL + "/use";

        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("orderId", orderId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, request, Object.class);
    }
}