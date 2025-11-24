package com.example.demo.vnpay;

import com.example.demo.vnpay.VNPayConfig;
import com.example.demo.news.News;
import com.example.demo.news.NewsRepository;
import com.example.demo.reservation.Reservation;
import com.example.demo.reservation.ReservationRepository;
import com.example.demo.showtime.Showtime;
import com.example.demo.showtime.ShowtimeRepository;
import com.example.demo.cinema.Cinema;
import com.example.demo.cinema.CinemaRepository;
import com.example.demo.userVoucherDetials.UserVoucherDetail;
import com.example.demo.userVoucherDetials.UserVoucherDetailRepository;
import com.example.demo.vnpay.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayConfig vnPayConfig;
    private final ReservationRepository reservationRepo;
    private final ShowtimeRepository showtimeRepo;
    private final CinemaRepository cinemaRepo;
    private final NewsRepository newsRepo;
    private final UserVoucherDetailRepository uvdRepo;

    /**
     * Tạo URL thanh toán VNPay
     * Endpoint: POST /api/payment/create-vnpay-payment
     */
    @PostMapping("/create-vnpay-payment")
    public ResponseEntity<?> createPayment(@RequestParam String reservationId,
                                           @RequestParam(required = false) String bankCode,
                                           HttpServletRequest request) { // Thêm HttpServletRequest
        try {
            // Lấy thông tin reservation
            Optional<Reservation> optReservation = reservationRepo.findById(reservationId);
            if (optReservation.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Reservation not found"));
            }

            Reservation reservation = optReservation.get();

            // Kiểm tra trạng thái thanh toán
            if (!"PENDING".equals(reservation.getPaymentStatus())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid payment status",
                        "currentStatus", reservation.getPaymentStatus()
                ));
            }

            // Tạo mã giao dịch (format: yyyyMMddHHmmss + random 6 số)
            String vnp_TxnRef = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + VNPayUtil.getRandomNumber(6);

            // Lưu mã giao dịch vào reservation
            reservation.setVnpayTransactionId(vnp_TxnRef);
            reservation.setUpdatedAt(new Date());
            reservationRepo.save(reservation);

            // Số tiền (VNPay yêu cầu nhân với 100)
            long amount = reservation.getTotal() * 100L;

            // Lấy địa chỉ IP của client
            String vnp_IpAddr = request.getRemoteAddr();

            // Tạo params
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnPayConfig.getVnpVersion());
            vnp_Params.put("vnp_Command", vnPayConfig.getVnpCommand());
            vnp_Params.put("vnp_TmnCode", vnPayConfig.getVnpTmnCode());
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");

            if (bankCode != null && !bankCode.isEmpty()) {
                vnp_Params.put("vnp_BankCode", bankCode);
            }

            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", "Thanh toan ve xem phim - Ma dat ve: " + reservationId);
            vnp_Params.put("vnp_OrderType", vnPayConfig.getOrderType());
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnpReturnUrl());
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr); // Sử dụng IP động

            // Thời gian tạo và hết hạn
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            vnp_Params.put("vnp_CreateDate", LocalDateTime.now().format(formatter));
            vnp_Params.put("vnp_ExpireDate", LocalDateTime.now().plusMinutes(15).format(formatter));

            // Sắp xếp và tạo chuỗi hash
            String hashData = VNPayUtil.hashAllFields(vnp_Params);
            String vnp_SecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getVnpHashSecret(), hashData);

            // Tạo query string
            StringBuilder query = new StringBuilder();
            for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
                query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()));
                query.append('=');
                query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
                query.append('&');
            }

            // Tạo payment URL
            String paymentUrl = vnPayConfig.getVnpUrl() + "?" + query.toString() + "vnp_SecureHash=" + vnp_SecureHash;

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", "00");
            response.put("message", "Success");
            response.put("paymentUrl", paymentUrl);
            response.put("reservationId", reservationId);
            response.put("vnpayTransactionId", vnp_TxnRef);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * IPN Callback từ VNPay (server-to-server)
     * VNPay sẽ gọi endpoint này để thông báo kết quả thanh toán
     */
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<?> vnpayIPN(@RequestParam Map<String, String> params) {
        try {
            // Lấy secure hash từ VNPay
            String vnp_SecureHash = params.get("vnp_SecureHash");

            // Remove secure hash và hash type khỏi params để tính toán
            Map<String, String> fields = new HashMap<>(params);
            fields.remove("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");

            // Tính toán secure hash
            String signValue = VNPayUtil.hashAllFields(fields);
            String calculatedHash = VNPayUtil.hmacSHA512(vnPayConfig.getVnpHashSecret(), signValue);

            // Verify secure hash
            if (!calculatedHash.equals(vnp_SecureHash)) {
                return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid signature"));
            }

            // Lấy thông tin từ params
            String vnp_TxnRef = params.get("vnp_TxnRef");
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_TransactionStatus = params.get("vnp_TransactionStatus");

            // Tìm reservation theo vnpayTransactionId
            List<Reservation> reservations = reservationRepo.findAll().stream()
                    .filter(r -> vnp_TxnRef.equals(r.getVnpayTransactionId()))
                    .collect(Collectors.toList());

            if (reservations.isEmpty()) {
                return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
            }

            Reservation reservation = reservations.get(0);

            // Kiểm tra trạng thái đã xử lý chưa
            if ("COMPLETED".equals(reservation.getPaymentStatus())) {
                return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order already confirmed"));
            }

            // Kiểm tra số tiền
            long amount = Long.parseLong(params.get("vnp_Amount")) / 100;
            if (amount != reservation.getTotal()) {
                return ResponseEntity.ok(Map.of("RspCode", "04", "Message", "Invalid amount"));
            }

            // Cập nhật trạng thái thanh toán
            if ("00".equals(vnp_ResponseCode) && "00".equals(vnp_TransactionStatus)) {
                // Thanh toán thành công
                reservation.setPaymentStatus("COMPLETED");
                reservation.setPaymentDate(new Date());
                reservation.setUpdatedAt(new Date());
                reservationRepo.save(reservation);

                // Đánh dấu voucher đã sử dụng
                if (StringUtils.hasText(reservation.getUserId()) &&
                        StringUtils.hasText(reservation.getVoucherId())) {
                    markVoucherAsUsed(reservation.getUserId(), reservation.getVoucherId());
                }

                // Tạo News notification
                createBookingNotification(reservation);

                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Success"));
            } else {
                // Thanh toán thất bại
                reservation.setPaymentStatus("FAILED");
                reservation.setUpdatedAt(new Date());
                reservationRepo.save(reservation);

                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm"));
            }

        } catch (Exception e) {
            System.err.println("VNPay IPN Error: " + e.getMessage());
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknown error"));
        }
    }

    /**
     * Endpoint để client kiểm tra kết quả thanh toán
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> params) {
        try {
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_TxnRef = params.get("vnp_TxnRef");
            String vnp_SecureHash = params.get("vnp_SecureHash");

            // Verify secure hash
            Map<String, String> fields = new HashMap<>(params);
            fields.remove("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");

            String signValue = VNPayUtil.hashAllFields(fields);
            String calculatedHash = VNPayUtil.hmacSHA512(vnPayConfig.getVnpHashSecret(), signValue);

            if (!calculatedHash.equals(vnp_SecureHash)) {
                return ResponseEntity.ok(Map.of(
                        "code", "97",
                        "message", "Invalid signature",
                        "success", false
                ));
            }

            // Tìm reservation
            List<Reservation> reservations = reservationRepo.findAll().stream()
                    .filter(r -> vnp_TxnRef.equals(r.getVnpayTransactionId()))
                    .collect(Collectors.toList());

            if (reservations.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", "01",
                        "message", "Order not found",
                        "success", false
                ));
            }

            Reservation reservation = reservations.get(0);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("responseCode", vnp_ResponseCode);
            response.put("transactionId", vnp_TxnRef);
            response.put("reservationId", reservation.getId());
            response.put("paymentStatus", reservation.getPaymentStatus());
            response.put("success", "00".equals(vnp_ResponseCode));

            if ("00".equals(vnp_ResponseCode)) {
                response.put("message", "Thanh toán thành công");
                response.put("QRCode", "https://elcinema.herokuapp.com/#/checkin/" + reservation.getId());
            } else {
                response.put("message", "Thanh toán thất bại");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", "99",
                    "message", "Unknown error: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Helper: Đánh dấu voucher đã sử dụng
     */
    private void markVoucherAsUsed(String userId, String voucherId) {
        try {
            uvdRepo.findByUserIdAndVoucherId(userId, voucherId).ifPresent(uvd -> {
                if (uvd.getStatus() != UserVoucherDetail.Status.USED) {
                    uvd.setStatus(UserVoucherDetail.Status.USED);
                    uvd.setUsedAt(new Date());
                    uvdRepo.save(uvd);
                }
            });
        } catch (Exception ex) {
            System.err.println("Mark voucher USED failed: " + ex.getMessage());
        }
    }

    /**
     * Helper: Tạo thông báo booking
     */
    private void createBookingNotification(Reservation reservation) {
        try {
            Showtime show = showtimeRepo.findById(reservation.getShowtimeId()).orElse(null);
            String cinemaName = "N/A";

            if (show != null && StringUtils.hasText(show.getCinemaId())) {
                cinemaName = cinemaRepo.findById(show.getCinemaId())
                        .map(Cinema::getName)
                        .orElse("N/A");
            }

            // Lấy danh sách ghế
            List<String> seatCodes = reservation.getSeats().stream()
                    .map(o -> {
                        if (o instanceof String) return (String) o;
                        if (o instanceof Map) {
                            Map<?, ?> m = (Map<?, ?>) o;
                            Object code = m.get("code");
                            return code != null ? code.toString() : null;
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Lấy danh sách đồ ăn
            List<Map<String, Object>> foodList = new ArrayList<>();
            if (reservation.getFood() != null) {
                for (Object o : reservation.getFood()) {
                    if (o instanceof Map) {
                        foodList.add((Map<String, Object>) o);
                    }
                }
            }

            int foodTotal = foodList.stream()
                    .mapToInt(f -> {
                        int price = f.get("price") != null ? ((Number) f.get("price")).intValue() : 0;
                        int qty = f.get("quantity") != null ? ((Number) f.get("quantity")).intValue() : 0;
                        return price * qty;
                    })
                    .sum();

            String foodListStr = foodList.isEmpty() ? "Không có" : foodList.stream()
                    .map(f -> String.format("%s x%d (%,dđ)",
                            f.getOrDefault("foodName", f.get("foodId")),
                            f.get("quantity"),
                            ((Number) f.get("price")).intValue() * ((Number) f.get("quantity")).intValue()))
                    .collect(Collectors.joining(", "));

            String showDateStr = new SimpleDateFormat("yyyy-MM-dd").format(reservation.getDate());
            Date bookedAt = reservation.getPaymentDate() != null ? reservation.getPaymentDate() : new Date();

            String description = String.format(
                    "Phòng chiếu: %s | Ghế: %s | Suất chiếu: %s %s | Đặt lúc: %s | Đồ ăn: %s | Tổng đồ ăn: %,dđ | Thanh toán: %s",
                    cinemaName,
                    String.join(", ", seatCodes),
                    show != null && StringUtils.hasText(show.getStartAt()) ? show.getStartAt() : reservation.getStartAt(),
                    showDateStr,
                    bookedAt.toInstant().atZone(ZoneId.of("Asia/Bangkok")).toLocalDateTime().toString().replace('T', ' '),
                    foodListStr,
                    foodTotal,
                    reservation.getPaymentMethod()
            );

            String newsUserId = null;
            if (StringUtils.hasText(reservation.getUserId()) && ObjectId.isValid(reservation.getUserId())) {
                newsUserId = reservation.getUserId();
            }

            News news = News.builder()
                    .title("Đặt vé thành công")
                    .description(description)
                    .date(bookedAt)
                    .active(1)
                    .userId(newsUserId)
                    .build();

            newsRepo.save(news);

        } catch (Exception ex) {
            System.err.println("Create booking notification failed: " + ex.getMessage());
        }
    }
}