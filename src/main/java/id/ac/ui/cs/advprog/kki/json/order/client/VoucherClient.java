package id.ac.ui.cs.advprog.kki.json.order.client;

import id.ac.ui.cs.advprog.kki.json.voucher.dto.ValidateVoucherResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component("orderVoucherClient")
public class VoucherClient {

    private final RestTemplate restTemplate;
    private final String BASE_URL = "http://localhost:8080/api/vouchers";

    public VoucherClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public double applyVoucher(String code, double total, String token) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("orderTotal", total);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ValidateVoucherResponse response = restTemplate.postForObject(
                BASE_URL + "/validate", request, ValidateVoucherResponse.class);

        return response != null ? response.getFinalTotal() : total;
    }

    public void useVoucher(String code, String orderId, String token) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("orderId", orderId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForObject(BASE_URL + "/use", request, Void.class);
    }
}