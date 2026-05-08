package id.ac.ui.cs.advprog.kki.json.client.voucher;

import id.ac.ui.cs.advprog.kki.json.voucher.dto.UseVoucherResponse;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.ValidateVoucherResponse;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.VoucherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * VoucherClient for the Order module to communicate with the Voucher service.
 * This client handles REST calls to voucher endpoints.
 */
@Component
public class VoucherClient {

    private final RestTemplate restTemplate;
    private final String voucherServiceUrl;

    public VoucherClient(RestTemplate restTemplate, 
                         @Value("${voucher.service.url:http://localhost:8080}") String voucherServiceUrl) {
        this.restTemplate = restTemplate;
        this.voucherServiceUrl = voucherServiceUrl;
    }

    /**
     * Validates a voucher with the given code and order total.
     *
     * @param voucherCode The voucher code
     * @param orderTotal The total order amount
     * @return ValidateVoucherResponse containing discount information
     * @throws ResponseStatusException if voucher is invalid or not found
     */
    public ValidateVoucherResponse validateVoucher(String voucherCode, Double orderTotal) {
        try {
            String url = voucherServiceUrl + "/api/vouchers/validate";
            ValidateVoucherRequest request = new ValidateVoucherRequest(voucherCode, orderTotal);
            return restTemplate.postForObject(url, request, ValidateVoucherResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Failed to validate voucher: " + e.getMessage());
        }
    }

    /**
     * Applies a voucher to an order. This will decrement the voucher quota.
     *
     * @param voucherCode The voucher code
     * @param orderId The order ID
     * @param userId The user ID
     * @return UseVoucherResponse containing the voucher application result
     * @throws ResponseStatusException if voucher cannot be applied
     */
    public UseVoucherResponse applyVoucher(String voucherCode, String orderId, String userId) {
        try {
            String url = voucherServiceUrl + "/api/vouchers/use";
            UseVoucherRequest request = new UseVoucherRequest(voucherCode, orderId);
            return restTemplate.postForObject(url, request, UseVoucherResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Failed to apply voucher: " + e.getMessage());
        }
    }

    /**
     * Retrieves a specific voucher by its code.
     *
     * @param voucherCode The voucher code
     * @return VoucherResponse containing voucher details
     * @throws ResponseStatusException if voucher is not found
     */
    public VoucherResponse getVoucherByCode(String voucherCode) {
        try {
            String url = voucherServiceUrl + "/api/vouchers/" + voucherCode;
            return restTemplate.getForObject(url, VoucherResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Voucher not found: " + e.getMessage());
        }
    }

    /**
     * Internal request DTO for validate voucher endpoint
     */
    private static class ValidateVoucherRequest {
        public String code;
        public Double orderTotal;

        public ValidateVoucherRequest(String code, Double orderTotal) {
            this.code = code;
            this.orderTotal = orderTotal;
        }
    }

    /**
     * Internal request DTO for apply/use voucher endpoint
     */
    private static class UseVoucherRequest {
        public String code;
        public String orderId;

        public UseVoucherRequest(String code, String orderId) {
            this.code = code;
            this.orderId = orderId;
        }
    }
}
