package com.example.demo.voucher;

import com.example.demo.userVoucherDetials.UserVoucherDetail;
import com.example.demo.userVoucherDetials.UserVoucherDetail.Status;
import com.example.demo.userVoucherDetials.UserVoucherDetailRepository;
import com.example.demo.voucher.Voucher;
import com.example.demo.voucher.VoucherRepository;
import lombok.*;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class VoucherApiController {

    private final VoucherRepository voucherRepo;
    private final UserVoucherDetailRepository uvdRepo;

    // ================== Helpers ==================

    /**
     * Parse linh hoạt chuỗi ngày:
     * - Ưu tiên Instant.parse (ISO-8601 đầy đủ: 2025-11-10T00:00:00.000Z, 2025-11-10T00:00:00+07:00, ...)
     * - Nếu chỉ yyyy-MM-dd, coi là 00:00:00 UTC (ổn định, tránh lệch timezone).
     * - Hỗ trợ thêm các mẫu: yyyy/MM/dd, dd-MM-yyyy, dd/MM/yyyy
     */
    private Instant parseFlexibleInstant(String input) {
        if (!StringUtils.hasText(input)) return null;

        // Trim các kí tự trắng/trailing
        String s = input.trim();

        // Một số chuỗi ISO có phần milli không đầy đủ: cho Instant.parse xử lý trước
        try {
            return Instant.parse(s);
        } catch (Exception ignore) {}

        // yyyy-MM-dd
        try {
            LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            return d.atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception ignore) {}

        // Các pattern phổ biến khác
        DateTimeFormatter[] fmts = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        };
        for (DateTimeFormatter f : fmts) {
            try {
                LocalDate d = LocalDate.parse(s, f);
                return d.atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (Exception ignore) {}
        }

        // Nếu vẫn không parse được
        throw new IllegalArgumentException("Invalid date format: " + input + ". Use ISO-8601 or yyyy-MM-dd.");
    }

    private Voucher.VoucherType safeParseType(String typeStr) {
        if (!StringUtils.hasText(typeStr)) return null;
        try {
            return Voucher.VoucherType.valueOf(typeStr.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid voucher type: " + typeStr + ". Use PERCENT or FIXED.");
        }
    }

    private boolean parseActive(String activeStr) {
        if (!StringUtils.hasText(activeStr)) return false;
        String s = activeStr.trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
    }

    // ================== VOUCHER CRUD ==================

    @PostMapping("/vouchers")
    public ResponseEntity<Voucher> createVoucher(@RequestBody VoucherCreateUpdate req) {
        Voucher v = mapToVoucher(req);
        Date now = Date.from(Instant.now());
        v.setCreatedAt(now);
        v.setUpdatedAt(now);
        return ResponseEntity.ok(voucherRepo.save(v));
    }

    @GetMapping("/vouchers")
    public ResponseEntity<List<Voucher>> listVouchers() {
        return ResponseEntity.ok(voucherRepo.findAll());
    }

    @GetMapping("/vouchers/{id}")
    public ResponseEntity<Voucher> getVoucher(@PathVariable String id) {
        return ResponseEntity.of(voucherRepo.findById(id));
    }

    @PutMapping("/vouchers/{id}")
    public ResponseEntity<Voucher> updateVoucher(@PathVariable String id, @RequestBody VoucherCreateUpdate req) {
        Voucher exist = voucherRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Voucher not found"));
        Voucher v = mapToVoucher(req);
        v.setId(id);
        v.setCreatedAt(exist.getCreatedAt());
        v.setUpdatedAt(Date.from(Instant.now()));
        return ResponseEntity.ok(voucherRepo.save(v));
    }

    @DeleteMapping("/vouchers/{id}")
    public ResponseEntity<Void> deleteVoucher(@PathVariable String id) {
        voucherRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ================== USER_VOUCHER_DETAIL CRUD ==================

    @PostMapping("/user-voucher-details")
    public ResponseEntity<?> createUvd(@RequestBody UserVoucherDetail d) {
        // Kiểm tra xem các ID cần thiết có được cung cấp không
        if (d.getUserId() == null || d.getVoucherId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId and voucherId are required"));
        }

        // Kiểm tra xem UserVoucherDetail với cặp userId và voucherId này đã tồn tại chưa
        Optional<UserVoucherDetail> existingUvd = uvdRepo.findByUserIdAndVoucherId(d.getUserId(), d.getVoucherId());

        if (existingUvd.isPresent()) {
            // Nếu đã tồn tại, trả về lỗi 409 Conflict
            return ResponseEntity.status(409).body(Map.of("error", "UserVoucherDetail already exists for this user and voucher."));
        }

        // Nếu chưa tồn tại, lưu và trả về đối tượng đã tạo
        UserVoucherDetail savedUvd = uvdRepo.save(d);
        return ResponseEntity.ok(savedUvd);
    }

    @GetMapping("/user-voucher-details")
    public ResponseEntity<List<UserVoucherDetail>> listUvd() {
        return ResponseEntity.ok(uvdRepo.findAll());
    }

    @GetMapping("/user-voucher-details/{id}")
    public ResponseEntity<UserVoucherDetail> getUvd(@PathVariable String id) {
        return ResponseEntity.of(uvdRepo.findById(id));
    }

    @PutMapping("/user-voucher-details/{id}")
    public ResponseEntity<UserVoucherDetail> updateUvd(@PathVariable String id, @RequestBody UserVoucherDetail d) {
        UserVoucherDetail exist = uvdRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("UVD not found"));
        d.setId(exist.getId());
        return ResponseEntity.ok(uvdRepo.save(d));
    }

    @DeleteMapping("/user-voucher-details/{id}")
    public ResponseEntity<Void> deleteUvd(@PathVariable String id) {
        uvdRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ================== ENDPOINT A: (voucherId) -> subset userIds CHƯA DÙNG voucher ==================
    /**
     * POST /api/v1/vouchers/{voucherId}/unused-user-ids
     * Body: { "candidates": ["u1","u2","u3"] }
     * Trả về: { "notUsedUserIds": ["u2","u3"] }
     */
    @PostMapping("/vouchers/{voucherId}/unused-user-ids")
    public ResponseEntity<UnusedUsersResponse> listUsersNotUsedVoucher(
            @PathVariable String voucherId,
            @RequestBody UnusedUsersRequest body
    ) {
        List<String> candidates = Optional.ofNullable(body.getCandidates()).orElseGet(List::of);
        if (candidates.isEmpty()) {
            return ResponseEntity.ok(new UnusedUsersResponse(List.of()));
        }

        List<UserVoucherDetail> usedForCandidates =
                uvdRepo.findByVoucherIdAndUserIdInAndStatus(voucherId, candidates, Status.USED);

        Set<String> usedIds = usedForCandidates.stream()
                .map(UserVoucherDetail::getUserId)
                .collect(Collectors.toSet());

        List<String> notUsed = candidates.stream()
                .filter(u -> !usedIds.contains(u))
                .toList();

        return ResponseEntity.ok(new UnusedUsersResponse(notUsed));
    }

    // ================== ENDPOINT B: (userId) -> danh sách voucher CHƯA dùng ==================
    /**
     * GET /api/v1/users/{userId}/unused-vouchers?onlyActive=true
     */
    @GetMapping("/users/{userId}/unused-vouchers")
    public ResponseEntity<List<Voucher>> listVouchersUserNotUsed(
            @PathVariable String userId,
            @RequestParam(name = "onlyActive", defaultValue = "true") boolean onlyActive
    ) {
        List<UserVoucherDetail> used = uvdRepo.findByUserIdAndStatus(userId, Status.USED);
        Set<String> usedVoucherIds = used.stream().map(UserVoucherDetail::getVoucherId).collect(Collectors.toSet());

        List<Voucher> base = onlyActive
                ? voucherRepo.findByActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqual(new Date(), new Date())
                : voucherRepo.findAll();

        List<Voucher> notUsed = base.stream()
                .filter(v -> !usedVoucherIds.contains(v.getId()))
                .toList();
        return ResponseEntity.ok(notUsed);
    }

    // ================== DTOs & Mapper ==================

    @Getter @Setter
    public static class VoucherCreateUpdate {
        public String id;
        public String code;
        public String name;
        public String description;
        public String type;        // "PERCENT" | "FIXED"
        public Integer value;
        public Integer maxDiscount;
        public Integer minOrderTotal;
        public String startAt;     // "2025-11-10" hoặc "2025-11-10T00:00:00.000Z"
        public String endAt;       // tương tự
        public String active;      // "1" | "0" | "true" | "false"
    }

    private Voucher mapToVoucher(VoucherCreateUpdate r) {
        Voucher.VoucherType t = safeParseType(r.type);
        boolean active = parseActive(r.active);

        Date start = null, end = null;
        if (StringUtils.hasText(r.startAt)) {
            start = Date.from(parseFlexibleInstant(r.startAt));
        }
        if (StringUtils.hasText(r.endAt)) {
            end = Date.from(parseFlexibleInstant(r.endAt));
        }

        // Nếu cả start và end đều có, kiểm tra logic thời gian
        if (start != null && end != null && end.before(start)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }

        return Voucher.builder()
                .id(r.id)
                .code(r.code)
                .name(r.name)
                .description(r.description)
                .type(t)
                .value(r.value)
                .maxDiscount(r.maxDiscount)
                .minOrderTotal(r.minOrderTotal)
                .startAt(start)
                .endAt(end)
                .active(active)
                .build();
    }

    @Getter @Setter
    public static class UnusedUsersRequest {
        private List<String> candidates;
    }

    @Getter @Setter @AllArgsConstructor
    public static class UnusedUsersResponse {
        private List<String> notUsedUserIds;
    }
}
