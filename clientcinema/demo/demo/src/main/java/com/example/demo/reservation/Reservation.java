package com.example.demo.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reservations")
public class Reservation {

    @Id
    private String id;

    private String showtimeId;
    private Date date;
    private String startAt;
    private List<Object> seats;
    private List<Object> food;
    private Integer total;
    private String username;
    private String phone;
    private Boolean checkin;

    // ========== THÊM CÁC TRƯỜNG MỚI CHO VNPAY ==========

    // Trạng thái thanh toán: PENDING, COMPLETED, FAILED, CANCELLED
    private String paymentStatus;

    // Phương thức thanh toán: VNPAY, CASH, MOMO, etc.
    private String paymentMethod;

    // Mã giao dịch VNPay
    private String vnpayTransactionId;

    // Thời gian thanh toán
    private Date paymentDate;

    // User ID (để đánh dấu voucher)
    private String userId;

    // Voucher ID đã sử dụng
    private String voucherId;

    // Thời gian tạo reservation
    private Date createdAt;

    // Thời gian cập nhật
    private Date updatedAt;
}