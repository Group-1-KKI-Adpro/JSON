package id.ac.ui.cs.advprog.kki.json.voucher;

import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.voucher.controller.VoucherController;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.UseVoucherResponse;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.ValidateVoucherResponse;
import id.ac.ui.cs.advprog.kki.json.voucher.model.DiscountType;
import id.ac.ui.cs.advprog.kki.json.voucher.model.Voucher;
import id.ac.ui.cs.advprog.kki.json.voucher.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VoucherControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VoucherService voucherService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private VoucherController voucherController;

    private Voucher sampleVoucher;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(voucherController).build();
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant nextWeek = Instant.now().plus(7, ChronoUnit.DAYS);
        sampleVoucher = new Voucher("SAVE10", 50, yesterday, nextWeek, "Min order 100K", DiscountType.FLAT, 10_000.0, true);
    }

    // ── GET /api/vouchers ─────────────────────────────────────────────────────

    @Test
    void getActiveVouchers_returnsOkWithVoucherList() throws Exception {
        when(voucherService.getActiveVouchers()).thenReturn(List.of(sampleVoucher));

        mockMvc.perform(get("/api/vouchers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SAVE10"))
                .andExpect(jsonPath("$[0].quota").value(50))
                .andExpect(jsonPath("$[0].discountType").value("FLAT"));
    }

    @Test
    void getActiveVouchers_emptyList_returnsOkWithEmptyArray() throws Exception {
        when(voucherService.getActiveVouchers()).thenReturn(List.of());

        mockMvc.perform(get("/api/vouchers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/vouchers/{code} ──────────────────────────────────────────────

    @Test
    void getVoucherByCode_found_returnsOk() throws Exception {
        when(voucherService.getVoucherByCode("SAVE10")).thenReturn(sampleVoucher);

        mockMvc.perform(get("/api/vouchers/SAVE10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE10"))
                .andExpect(jsonPath("$.discountValue").value(10_000.0));
    }

    @Test
    void getVoucherByCode_notFound_returns404() throws Exception {
        when(voucherService.getVoucherByCode("GHOST"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found"));

        mockMvc.perform(get("/api/vouchers/GHOST"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/vouchers/validate ───────────────────────────────────────────

    @Test
    void validateVoucher_validCode_returnsDiscountBreakdown() throws Exception {
        ValidateVoucherResponse response = new ValidateVoucherResponse("SAVE10", 100_000.0, DiscountType.FLAT, 10_000.0, 90_000.0);
        when(voucherService.validateVoucher("SAVE10", 100_000.0)).thenReturn(response);

        mockMvc.perform(post("/api/vouchers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SAVE10\",\"orderTotal\":100000.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE10"))
                .andExpect(jsonPath("$.discountAmount").value(10_000.0))
                .andExpect(jsonPath("$.finalTotal").value(90_000.0));
    }

    @Test
    void validateVoucher_expiredCode_returns400() throws Exception {
        when(voucherService.validateVoucher(eq("OLD"), anyDouble()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is expired"));

        mockMvc.perform(post("/api/vouchers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"OLD\",\"orderTotal\":100000.0}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/vouchers/use ────────────────────────────────────────────────

    @Test
    void useVoucher_authenticated_returnsOkWithRemainingQuota() throws Exception {
        User user = new User("user@test.com", "hash", "Test User");
        when(authService.getByEmail("user@test.com")).thenReturn(user);
        when(voucherService.useVoucher(eq("SAVE10"), eq("order-1"), any()))
                .thenReturn(new UseVoucherResponse("SAVE10", "order-1", "1", 49));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(post("/api/vouchers/use")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SAVE10\",\"orderId\":\"order-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE10"))
                .andExpect(jsonPath("$.remainingQuota").value(49));
    }

    @Test
    void useVoucher_exhaustedQuota_returns400() throws Exception {
        User user = new User("user@test.com", "hash", "Test User");
        when(authService.getByEmail("user@test.com")).thenReturn(user);
        when(voucherService.useVoucher(eq("EMPTY"), eq("order-2"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher quota is exhausted"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(post("/api/vouchers/use")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"EMPTY\",\"orderId\":\"order-2\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/admin/vouchers ──────────────────────────────────────────────

    @Test
    void createVoucher_validRequest_returnsCreated() throws Exception {
        when(voucherService.createVoucher(any())).thenReturn(sampleVoucher);

        mockMvc.perform(post("/api/admin/vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"SAVE10",
                                  "quota":50,
                                  "startAt":"2026-01-01T00:00:00Z",
                                  "endAt":"2026-12-31T00:00:00Z",
                                  "discountType":"FLAT",
                                  "discountValue":10000.0,
                                  "terms":"Min order 100K"
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SAVE10"));
    }

    // ── PATCH /api/admin/vouchers/{code} ──────────────────────────────────────

    @Test
    void updateVoucher_validRequest_returnsUpdatedVoucher() throws Exception {
        Instant nextWeek = Instant.now().plus(7, ChronoUnit.DAYS);
        Voucher updated = new Voucher("SAVE10", 60,
                Instant.now().minus(1, ChronoUnit.DAYS),
                nextWeek.plus(7, ChronoUnit.DAYS),
                "terms", DiscountType.FLAT, 10_000.0, true);
        when(voucherService.updateVoucher(eq("SAVE10"), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/admin/vouchers/SAVE10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quotaToAdd\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quota").value(60));
    }
}
