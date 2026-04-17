package id.ac.ui.cs.advprog.kki.json.order.client;

import id.ac.ui.cs.advprog.kki.json.wallet.dto.BalanceResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WalletClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String BASE_URL = "http://localhost:8080/api/wallet";

    public long getBalance(String token) {
        String url = BASE_URL + "/balance";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<BalanceResponse> response =
                restTemplate.exchange(url, HttpMethod.GET, request, BalanceResponse.class);

        return response.getBody().balance();
    }

    public void refund(Long userId, Long amount) {
        // Not supported in wallet module yet → safe no-op
    }
}
