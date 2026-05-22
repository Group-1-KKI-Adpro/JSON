package id.ac.ui.cs.advprog.kki.json.order.client;

import id.ac.ui.cs.advprog.kki.json.wallet.dto.BalanceResponse;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.DeductRequest;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.RefundRequest;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.TransactionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WalletClient {

    private final RestTemplate restTemplate;
    private final String walletApiBaseUrl;
    private final String internalServiceToken;

    public WalletClient(RestTemplate restTemplate,
                        @Value("${app.wallet.base-url:http://localhost:8080/api}") String walletApiBaseUrl,
                        @Value("${app.internal.service-token:dev-internal-service-token}") String internalServiceToken) {
        this.restTemplate = restTemplate;
        this.walletApiBaseUrl = walletApiBaseUrl;
        this.internalServiceToken = internalServiceToken;
    }

    public long getBalance(String token) {
        ResponseEntity<BalanceResponse> response = restTemplate.exchange(
                walletApiBaseUrl + "/wallet/balance",
                HttpMethod.GET,
                bearerRequest(token),
                BalanceResponse.class
        );

        return response.getBody().balance();
    }

    public TransactionResponse deduct(Long userId,
                                      Long amount,
                                      String referenceId,
                                      String description) {
        return postInternal(
                "/internal/wallet/deduct",
                new DeductRequest(userId, amount, referenceId, description),
                TransactionResponse.class
        );
    }

    public TransactionResponse refund(Long userId,
                                      Long amount,
                                      String referenceId,
                                      String description) {
        return postInternal(
                "/internal/wallet/refund",
                new RefundRequest(userId, amount, referenceId, description),
                TransactionResponse.class
        );
    }

    private HttpEntity<Void> bearerRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private <T> T postInternal(String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Service-Token", internalServiceToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(walletApiBaseUrl + path, request, responseType);
    }
}
