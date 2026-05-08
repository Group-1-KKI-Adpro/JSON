package id.ac.ui.cs.advprog.kki.json.order.client;

// NPM: CAPEK2406365364

import id.ac.ui.cs.advprog.kki.json.voucher.dto.ValidateVoucherResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component("orderVoucherClient")
public class VoucherClient {

    private final WebClient webClient;
    private final String BASE_URL = "http://localhost:8080/api/vouchers";

    public VoucherClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
    }

    public double applyVoucher(String code, double total) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("orderTotal", total);

        ValidateVoucherResponse response = webClient.post()
                .uri("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ValidateVoucherResponse.class)
                .block(); // Menunggu respon secara sinkron

        return response != null ? response.getFinalTotal() : total;
    }

    public void useVoucher(String code, String orderId, String token) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("orderId", orderId);

        webClient.post()
                .uri("/use")
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                          response -> Mono.error(new RuntimeException("Voucher use failed")))
                .bodyToMono(Void.class)
                .block();
    }
}