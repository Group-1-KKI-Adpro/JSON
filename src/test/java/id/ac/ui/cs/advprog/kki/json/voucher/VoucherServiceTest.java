package id.ac.ui.cs.advprog.kki.json.voucher;

import id.ac.ui.cs.advprog.kki.json.voucher.dto.CreateVoucherRequest;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.UpdateVoucherRequest;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.UseVoucherResponse;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.ValidateVoucherResponse;
import id.ac.ui.cs.advprog.kki.json.voucher.model.DiscountType;
import id.ac.ui.cs.advprog.kki.json.voucher.model.Voucher;
import id.ac.ui.cs.advprog.kki.json.voucher.repository.VoucherRepository;
import id.ac.ui.cs.advprog.kki.json.voucher.service.VoucherService;
import id.ac.ui.cs.advprog.kki.json.voucher.strategy.FlatDiscountStrategy;
import id.ac.ui.cs.advprog.kki.json.voucher.strategy.PercentageDiscountStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    private VoucherService voucherService;

    private Instant yesterday;
    private Instant nextWeek;

    @BeforeEach
    void setUp() {
        voucherService = new VoucherService(
                voucherRepository,
                List.of(new FlatDiscountStrategy(), new PercentageDiscountStrategy())
        );
        Instant now = Instant.now();
        yesterday = now.minus(1, ChronoUnit.DAYS);
        nextWeek = now.plus(7, ChronoUnit.DAYS);
    }

    private Voucher activeVoucher(String code, DiscountType type, double value, int quota) {
        return new Voucher(code, quota, yesterday, nextWeek, "terms", type, value, true);
    }

    // ── createVoucher ─────────────────────────────────────────────────────────

    @Test
    void createVoucher_success_returnsNewVoucher() {
        CreateVoucherRequest req = new CreateVoucherRequest(
                "SAVE10", 100, yesterday, nextWeek, "terms", DiscountType.FLAT, 10.0);
        when(voucherRepository.existsById("SAVE10")).thenReturn(false);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> inv.getArgument(0));

        Voucher result = voucherService.createVoucher(req);

        assertEquals("SAVE10", result.getCode());
        assertEquals(100, result.getQuota());
        assertTrue(result.getActive());
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    void createVoucher_duplicateCode_throwsConflict() {
        CreateVoucherRequest req = new CreateVoucherRequest(
                "DUPE", 10, yesterday, nextWeek, null, DiscountType.FLAT, 5.0);
        when(voucherRepository.existsById("DUPE")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> voucherService.createVoucher(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(voucherRepository, never()).save(any());
    }

    // ── getActiveVouchers ─────────────────────────────────────────────────────

    @Test
    void getActiveVouchers_returnsListFromRepository() {
        List<Voucher> list = List.of(
                activeVoucher("V1", DiscountType.FLAT, 10, 5),
                activeVoucher("V2", DiscountType.PERCENTAGE, 20, 10)
        );
        when(voucherRepository.findByActiveTrue()).thenReturn(list);

        List<Voucher> result = voucherService.getActiveVouchers();

        assertEquals(2, result.size());
        verify(voucherRepository).findByActiveTrue();
    }

    // ── getVoucherByCode ──────────────────────────────────────────────────────

    @Test
    void getVoucherByCode_found_returnsVoucher() {
        Voucher v = activeVoucher("FOUND", DiscountType.FLAT, 10, 5);
        when(voucherRepository.findById("FOUND")).thenReturn(Optional.of(v));

        Voucher result = voucherService.getVoucherByCode("FOUND");

        assertEquals("FOUND", result.getCode());
    }

    @Test
    void getVoucherByCode_notFound_throws404() {
        when(voucherRepository.findById("MISSING")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> voucherService.getVoucherByCode("MISSING"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ── validateVoucher ───────────────────────────────────────────────────────

    @Test
    void validateVoucher_flatDiscount_calculatesCorrectly() {
        Voucher v = activeVoucher("FLAT10", DiscountType.FLAT, 10_000.0, 5);
        when(voucherRepository.findById("FLAT10")).thenReturn(Optional.of(v));

        ValidateVoucherResponse res = voucherService.validateVoucher("FLAT10", 100_000.0);

        assertEquals(10_000.0, res.getDiscountAmount());
        assertEquals(90_000.0, res.getFinalTotal());
    }

    @Test
    void validateVoucher_flatDiscount_cappedAtOrderTotal() {
        Voucher v = activeVoucher("FLAT200", DiscountType.FLAT, 200_000.0, 5);
        when(voucherRepository.findById("FLAT200")).thenReturn(Optional.of(v));

        ValidateVoucherResponse res = voucherService.validateVoucher("FLAT200", 100_000.0);

        assertEquals(100_000.0, res.getDiscountAmount());
        assertEquals(0.0, res.getFinalTotal());
    }

    @Test
    void validateVoucher_percentageDiscount_calculatesCorrectly() {
        Voucher v = activeVoucher("PCT20", DiscountType.PERCENTAGE, 20.0, 5);
        when(voucherRepository.findById("PCT20")).thenReturn(Optional.of(v));

        ValidateVoucherResponse res = voucherService.validateVoucher("PCT20", 100_000.0);

        assertEquals(20_000.0, res.getDiscountAmount());
        assertEquals(80_000.0, res.getFinalTotal());
    }

    @Test
    void validateVoucher_expiredVoucher_throws400() {
        Instant longAgo = Instant.now().minus(10, ChronoUnit.DAYS);
        Voucher v = new Voucher("EXPIRED", 5, longAgo, yesterday, "t", DiscountType.FLAT, 10.0, true);
        when(voucherRepository.findById("EXPIRED")).thenReturn(Optional.of(v));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> voucherService.validateVoucher("EXPIRED", 100_000.0));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void validateVoucher_inactiveVoucher_throws400() {
        Voucher v = new Voucher("INACTIVE", 5, yesterday, nextWeek, "t", DiscountType.FLAT, 10.0, false);
        when(voucherRepository.findById("INACTIVE")).thenReturn(Optional.of(v));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> voucherService.validateVoucher("INACTIVE", 100_000.0));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void validateVoucher_notStartedYet_throws400() {
        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);
        Voucher v = new Voucher("FUTURE", 5, tomorrow, nextWeek, "t", DiscountType.FLAT, 10.0, true);
        when(voucherRepository.findById("FUTURE")).thenReturn(Optional.of(v));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> voucherService.validateVoucher("FUTURE", 100_000.0));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // ── useVoucher ────────────────────────────────────────────────────────────

    @Test
    void useVoucher_success_decrementsQuota() {
        Voucher v = activeVoucher("USE10", DiscountType.FLAT, 10_000.0, 5);
        when(voucherRepository.findByIdForUpdate("USE10")).thenReturn(Optional.of(v));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> inv.getArgument(0));

        UseVoucherResponse res = voucherService.useVoucher("USE10", "order-1", "user-1");

        assertEquals("USE10", res.getCode());
        assertEquals(4, res.getRemainingQuota());
        verify(voucherRepository).save(v);
    }

    @Test
    void useVoucher_zeroQuota_throws400() {
        Voucher v = activeVoucher("EMPTY", DiscountType.FLAT, 10_000.0, 0);
        when(voucherRepository.findByIdForUpdate("EMPTY")).thenReturn(Optional.of(v));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> voucherService.useVoucher("EMPTY", "order-1", "user-1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    void useVoucher_expiredVoucher_throws400() {
        Instant longAgo = Instant.now().minus(10, ChronoUnit.DAYS);
        Voucher v = new Voucher("EXPUSED", 5, longAgo, yesterday, "t", DiscountType.FLAT, 10.0, true);
        when(voucherRepository.findByIdForUpdate("EXPUSED")).thenReturn(Optional.of(v));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> voucherService.useVoucher("EXPUSED", "order-1", "user-1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    void useVoucher_notFound_throws404() {
        when(voucherRepository.findByIdForUpdate("GHOST")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> voucherService.useVoucher("GHOST", "order-1", "user-1"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ── updateVoucher ─────────────────────────────────────────────────────────

    @Test
    void updateVoucher_extendEndAt_updatesDate() {
        Voucher v = activeVoucher("UPD1", DiscountType.FLAT, 10_000.0, 5);
        when(voucherRepository.findById("UPD1")).thenReturn(Optional.of(v));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant newEnd = nextWeek.plus(7, ChronoUnit.DAYS);
        Voucher result = voucherService.updateVoucher("UPD1", new UpdateVoucherRequest(newEnd, null, null));

        assertEquals(newEnd, result.getEndAt());
    }

    @Test
    void updateVoucher_addQuota_increasesTotalQuota() {
        Voucher v = activeVoucher("UPD2", DiscountType.FLAT, 10_000.0, 5);
        when(voucherRepository.findById("UPD2")).thenReturn(Optional.of(v));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> inv.getArgument(0));

        Voucher result = voucherService.updateVoucher("UPD2", new UpdateVoucherRequest(null, 10, null));

        assertEquals(15, result.getQuota());
    }

    @Test
    void updateVoucher_addQuotaToExpiredVoucher_throws400() {
        Instant longAgo = Instant.now().minus(10, ChronoUnit.DAYS);
        Voucher expired = new Voucher("EXP2", 5, longAgo, yesterday, "t", DiscountType.FLAT, 10.0, true);
        when(voucherRepository.findById("EXP2")).thenReturn(Optional.of(expired));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> voucherService.updateVoucher("EXP2", new UpdateVoucherRequest(null, 5, null)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateVoucher_deactivate_setsActiveFalse() {
        Voucher v = activeVoucher("UPD3", DiscountType.FLAT, 10_000.0, 5);
        when(voucherRepository.findById("UPD3")).thenReturn(Optional.of(v));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> inv.getArgument(0));

        Voucher result = voucherService.updateVoucher("UPD3", new UpdateVoucherRequest(null, null, false));

        assertFalse(result.getActive());
    }
}
