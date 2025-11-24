package com.example.demo.reservation;

import com.example.demo.cinema.Cinema;
import com.example.demo.cinema.CinemaRepository;
import com.example.demo.news.News;
import com.example.demo.news.NewsRepository;
import com.example.demo.showtime.Showtime;
import com.example.demo.showtime.ShowtimeRepository;

// 👉 Thêm các import cho UserVoucherDetail
import com.example.demo.userVoucherDetials.UserVoucherDetail;
import com.example.demo.userVoucherDetials.UserVoucherDetail.Status;
import com.example.demo.userVoucherDetials.UserVoucherDetailRepository;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationRepository repo;
    private final ShowtimeRepository showtimeRepo;
    private final CinemaRepository cinemaRepo;
    private final NewsRepository newsRepo;

    // 👉 Thêm repository để cập nhật trạng thái voucher
    private final UserVoucherDetailRepository uvdRepo;

    /* ===================== Helpers ===================== */

    /** Parse ngày từ nhiều dạng (ISO-8601, epoch millis, java.util.Date). Trả null nếu không hợp lệ. */
    private Date parseDate(Object v) {
        try {
            if (v instanceof String s) {
                // Hỗ trợ ISO "2025-11-15T00:00:00.000Z" hoặc "2025-11-15"
                if (s.contains("T")) {
                    return Date.from(Instant.parse(s));
                } else {
                    LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
                    return Date.from(ld.atStartOfDay(ZoneId.of("Asia/Bangkok")).toInstant());
                }
            } else if (v instanceof Number n) {
                return new Date(n.longValue()); // epoch millis
            } else if (v instanceof Date d) {
                return d;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private List<String> normalizeSeats(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof String s) {
                String v = s.trim().toUpperCase(Locale.ROOT);
                if (!v.isBlank()) out.add(v);
            } else if (o instanceof Map<?,?> m) {
                Object code = m.get("code");
                if (code instanceof String s) {
                    String v = s.trim().toUpperCase(Locale.ROOT);
                    if (!v.isBlank()) out.add(v);
                }
            }
        }
        return out;
    }

    private int asInt(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        try { return v == null ? 0 : Integer.parseInt(String.valueOf(v)); }
        catch (Exception ignored) { return 0; }
    }

    private String asString(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    /** Chấp nhận cả body.foods (List) hoặc body.food (Map/List) giống Node. */
    private List<Map<String,Object>> normalizeFood(Object foods, Object food) {
        List<?> src =
                (foods instanceof List<?> l1) ? l1 :
                        (food  instanceof List<?> l2) ? l2 :
                                (food  instanceof Map<?,?> m ) ? List.of(m) : List.of();

        List<Map<String,Object>> out = new ArrayList<>();
        for (Object o : src) {
            if (o instanceof Map<?,?> m) {
                Map<String,Object> item = new LinkedHashMap<>();
                Object foodId   = m.get("foodId");
                Object foodName = m.get("foodName"); // có thể null (client chỉ gửi id/price/quantity)
                Object price    = m.get("price");
                Object quantity = m.get("quantity");
                if (foodId != null)   item.put("foodId", String.valueOf(foodId));
                if (foodName != null) item.put("foodName", String.valueOf(foodName));
                item.put("price",    asInt(price));
                item.put("quantity", asInt(quantity));
                out.add(item);
            }
        }
        return out;
    }

    /* ===================== CREATE ===================== */

    // THAY THẾ method create() trong ReservationController.java hiện tại bằng code này:

    @PostMapping("/reservations")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> body) {
        try {
            Object showtimeId = body.get("showtimeId");
            Object dateRaw    = body.get("date");
            Object startAt    = body.get("startAt");
            Object username   = body.get("username");
            Object phone      = body.get("phone");

            if (!(showtimeId instanceof String s1 && !s1.isBlank()) ||
                    dateRaw == null ||
                    !(startAt instanceof String s2 && !s2.isBlank()) ||
                    !(username instanceof String s3 && !s3.isBlank()) ||
                    !(phone instanceof String s4 && !s4.isBlank())) {
                return ResponseEntity.badRequest().body(Map.of("error","MISSING_FIELDS"));
            }

            Date day = parseDate(dateRaw);
            if (day == null) return ResponseEntity.badRequest().body(Map.of("error","INVALID_DATE"));

            List<String> seats = normalizeSeats(body.get("seats"));
            if (seats.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","EMPTY_SEATS"));

            List<Map<String,Object>> foodItems = normalizeFood(body.get("foods"), body.get("food"));

            Object totalObj = body.get("total");
            if (!(totalObj instanceof Number) || ((Number) totalObj).intValue() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error","INVALID_TOTAL"));
            }

            // Lấy phương thức thanh toán (mặc định CASH nếu không có)
            String paymentMethod = asString(body.get("paymentMethod"));
            if (paymentMethod == null) {
                paymentMethod = "CASH"; // Mặc định thanh toán tiền mặt
            }

            // Tính khoảng ngày [00:00, +1d) theo Asia/Bangkok để so suất chiếu cùng ngày
            ZonedDateTime zStart = day.toInstant().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate()
                    .atStartOfDay(ZoneId.of("Asia/Bangkok"));
            Date dayStart = Date.from(zStart.toInstant());
            Date dayEnd   = Date.from(zStart.plusDays(1).toInstant());

            // Chặn trùng ghế theo showtimeId + startAt + date
            List<Reservation> sameSlot = repo.findAll().stream()
                    .filter(r -> Objects.equals(r.getShowtimeId(), String.valueOf(showtimeId)))
                    .filter(r -> Objects.equals(r.getStartAt(), String.valueOf(startAt)))
                    .filter(r -> r.getDate() != null && !r.getDate().before(dayStart) && r.getDate().before(dayEnd))
                    .collect(Collectors.toList());

            Set<String> seatTaken = new LinkedHashSet<>();
            for (Reservation r : sameSlot) {
                for (Object s : r.getSeats()) {
                    String val = (s instanceof String ss) ? ss :
                            (s instanceof Map<?,?> m && m.get("code") instanceof String cs) ? cs : null;
                    if (val != null && seats.contains(val)) seatTaken.add(val);
                }
            }
            if (!seatTaken.isEmpty()) {
                return ResponseEntity.status(409).body(Map.of("error","SEAT_TAKEN","seats", new ArrayList<>(seatTaken)));
            }

            // Lấy userId và voucherId (nếu có)
            String userId    = asString(body.get("userId"));
            String voucherId = asString(body.get("voucherId"));

            // Tạo reservation với trạng thái phù hợp
            String paymentStatus;
            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                paymentStatus = "PENDING"; // Chờ thanh toán online
            } else {
                paymentStatus = "COMPLETED"; // Thanh toán tiền mặt -> hoàn tất luôn
            }

            Reservation r = Reservation.builder()
                    .showtimeId(String.valueOf(showtimeId))
                    .date(day)
                    .startAt(String.valueOf(startAt))
                    .seats(new ArrayList<>(seats))
                    .food(new ArrayList<>(foodItems))
                    .total(((Number) totalObj).intValue())
                    .username(String.valueOf(username))
                    .phone(String.valueOf(phone))
                    .checkin(Boolean.TRUE.equals(body.get("checkin")))
                    // ====== THÊM CÁC TRƯỜNG MỚI ======
                    .paymentMethod(paymentMethod)
                    .paymentStatus(paymentStatus)
                    .userId(userId)
                    .voucherId(voucherId)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();

            r = repo.save(r);


            // QR code
            String QRCode = "https://elcinema.herokuapp.com/#/checkin/" + r.getId();
            try {
                Showtime show = showtimeRepo.findById(String.valueOf(showtimeId)).orElse(null);
                String cinemaName = "N/A";
                if (show != null && StringUtils.hasText(show.getCinemaId())) {
                    cinemaName = cinemaRepo.findById(show.getCinemaId()).map(Cinema::getName).orElse("N/A");
                }

                List<String> seatCodes = r.getSeats().stream()
                        .map(o -> (o instanceof String ss) ? ss
                                : (o instanceof Map<?,?> m && m.get("code") instanceof String cs) ? cs
                                : null)
                        .filter(Objects::nonNull).toList();

                List<Map<String,Object>> foodsSrc = new ArrayList<>();
                if (r.getFood() != null) {
                    for (Object o : r.getFood()) {
                        if (o instanceof Map<?,?> m) {
                            Map<String,Object> x = new LinkedHashMap<>();
                            x.put("foodId",   m.get("foodId"));
                            x.put("foodName", m.get("foodName"));
                            x.put("price",    asInt(m.get("price")));
                            x.put("quantity", asInt(m.get("quantity")));
                            foodsSrc.add(x);
                        }
                    }
                } else {
                    foodsSrc = normalizeFood(body.get("foods"), body.get("food"));
                }

                List<Map<String,Object>> foodsMapped = new ArrayList<>();
                for (Map<String,Object> f : foodsSrc) {
                    Map<String,Object> it = new LinkedHashMap<>();
                    it.put("foodId", f.get("foodId"));
                    Object nameRaw = f.get("foodName") != null ? f.get("foodName") : f.get("name");
                    it.put("name",   nameRaw != null ? String.valueOf(nameRaw) : "");
                    int priceVal    = asInt(f.get("price"));
                    int quantityVal = asInt(f.get("quantity"));
                    it.put("price", priceVal);
                    it.put("quantity", quantityVal);
                    foodsMapped.add(it);
                }

                String foodListStr = foodsMapped.isEmpty()
                        ? "Không có"
                        : foodsMapped.stream()
                        .map(f -> String.format("%s x%d (%sđ)",
                                Objects.toString(f.get("name"), Objects.toString(f.get("foodId"), "")),
                                asInt(f.get("quantity")),
                                String.format(Locale.forLanguageTag("vi-VN"), "%,d",
                                        asInt(f.get("price")) * asInt(f.get("quantity")))))
                        .collect(Collectors.joining(", "));

                int foodTotal = foodsMapped.stream()
                        .mapToInt(f -> asInt(f.get("price")) * asInt(f.get("quantity")))
                        .sum();

                String showDateStr = new SimpleDateFormat("yyyy-MM-dd").format(r.getDate());
                Date bookedAt = new Date();
                if (StringUtils.hasText(userId) && StringUtils.hasText(voucherId)) {
                        uvdRepo.findByUserIdAndVoucherId(userId, voucherId).ifPresent(uvd -> {
                            if (uvd.getStatus() != Status.USED) {
                                uvd.setStatus(Status.USED);
                                uvd.setUsedAt(new Date());
                                uvdRepo.save(uvd);
                            }
                        });
                }
                String description =
                        "Phòng chiếu: " + cinemaName + " | " +
                                "Ghế: " + String.join(", ", seatCodes) + " | " +
                                "Suất chiếu: " + (show != null && StringUtils.hasText(show.getStartAt()) ? show.getStartAt() : r.getStartAt()) + " " + showDateStr + " | " +
                                "Đặt lúc: " + bookedAt.toInstant().atZone(java.time.ZoneId.of("Asia/Bangkok")).toLocalDateTime().toString().replace('T',' ') + " | " +
                                "Đồ ăn: " + foodListStr + " | Tổng đồ ăn: " +
                                String.format(Locale.forLanguageTag("vi-VN"), "%,d", foodTotal) + "đ";

                String newsUserId = null;
                if (body.containsKey("userId") && body.get("userId") != null && !Objects.toString(body.get("userId")).isBlank()) {
                    String raw = Objects.toString(body.get("userId"));
                    if (ObjectId.isValid(raw)) newsUserId = raw;
                }

                News news = News.builder()
                        .title("đặt vé thành công")
                        .description(description)
                        .date(bookedAt)
                        .active(1)
                        .userId(newsUserId)
                        .build();
                newsRepo.save(news);
            } catch (Exception ex) {
                System.err.println("Create booking notification failed: " + ex.getMessage());
            }

            /* ================== END tạo News ================== */

            Map<String,Object> res = new LinkedHashMap<>();
            res.put("reservation", r);
            res.put("QRCode", QRCode);
            res.put("paymentStatus", paymentStatus);
            res.put("paymentMethod", paymentMethod);

            // Nếu là VNPAY -> client cần gọi API tạo payment URL
            if ("PENDING".equals(paymentStatus)) {
                res.put("message", "Reservation created. Please proceed to payment.");
                res.put("nextStep", "/api/payment/create-vnpay-payment?reservationId=" + r.getId());
            }

            return ResponseEntity.status(201).body(res);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /* ===================== READS ===================== */

    @GetMapping("/reservations")
    public ResponseEntity<?> findAll() {
        try { return ResponseEntity.ok(repo.findAll()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e); }
    }

    @GetMapping("/reservations/occupied")
    public ResponseEntity<?> occupied(@RequestParam String showtimeId,
                                      @RequestParam String date,
                                      @RequestParam String startAt) {
        try {
            LocalDate ld = LocalDate.parse(date);
            ZonedDateTime zStart = ld.atStartOfDay(ZoneId.of("Asia/Bangkok"));
            Date dayStart = Date.from(zStart.toInstant());
            Date dayEnd   = Date.from(zStart.plusDays(1).toInstant());

            List<Reservation> rs = repo.findAll().stream()
                    .filter(r -> Objects.equals(r.getShowtimeId(), showtimeId))
                    .filter(r -> Objects.equals(r.getStartAt(), startAt))
                    .filter(r -> r.getDate() != null && !r.getDate().before(dayStart) && r.getDate().before(dayEnd))
                    .collect(Collectors.toList());

            Set<String> occ = new LinkedHashSet<>();
            for (Reservation r : rs) {
                for (Object s : r.getSeats()) {
                    if (s instanceof String ss) occ.add(ss);
                    else if (s instanceof Map<?,?> m && m.get("code") instanceof String cs) occ.add(cs);
                }
            }

            Map<String,Object> out = new LinkedHashMap<>();
            out.put("showtimeId", showtimeId);
            out.put("date", date);
            out.put("startAt", startAt);
            out.put("occupied", new ArrayList<>(occ));
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/reservations/{id}")
    public ResponseEntity<?> findById(@PathVariable String id) {
        try {
            return repo.findById(id).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(404).build());
        } catch (Exception e) { return ResponseEntity.badRequest().body(e); }
    }

    @GetMapping("/reservations/checkin/{id}")
    public ResponseEntity<?> checkin(@PathVariable String id) {
        try {
            Optional<Reservation> or = repo.findById(id);
            if (or.isEmpty()) return ResponseEntity.status(404).build();
            Reservation r = or.get(); r.setCheckin(true);
            return ResponseEntity.ok(repo.save(r));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e); }
    }

    /* ===================== UPDATE / DELETE ===================== */

    @PatchMapping("/reservations/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String,Object> body) {
        Set<String> allowed = Set.of("date","startAt","seats","ticketPrice","total","username","phone","checkin");
        for (String k: body.keySet()) if (!allowed.contains(k))
            return ResponseEntity.badRequest().body(Map.of("error","Invalid updates!"));
        try {
            Optional<Reservation> or = repo.findById(id);
            if (or.isEmpty()) return ResponseEntity.status(404).build();
            Reservation r = or.get();
            if (body.containsKey("date")) {
                Date d = parseDate(body.get("date"));
                if (d != null) r.setDate(d);
            }
            if (body.containsKey("startAt"))   r.setStartAt(Objects.toString(body.get("startAt"), r.getStartAt()));
            if (body.containsKey("seats"))     r.setSeats((List<Object>) body.get("seats"));
            if (body.containsKey("total"))     r.setTotal(asInt(body.get("total")));
            if (body.containsKey("username"))  r.setUsername(Objects.toString(body.get("username"), r.getUsername()));
            if (body.containsKey("phone"))     r.setPhone(Objects.toString(body.get("phone"), r.getPhone()));
            if (body.containsKey("checkin"))   r.setCheckin(Boolean.TRUE.equals(body.get("checkin")));
            return ResponseEntity.ok(repo.save(r));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            Optional<Reservation> or = repo.findById(id);
            if (or.isEmpty()) return ResponseEntity.status(404).build();
            repo.deleteById(id);
            return ResponseEntity.ok(or.get());
        } catch (Exception e) { return ResponseEntity.status(400).build(); }
    }

    @GetMapping("/reservations/usermodeling/{username}")
    public ResponseEntity<?> userModeling(@PathVariable String username) {
        try { return ResponseEntity.ok(List.of()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e); }
    }
}