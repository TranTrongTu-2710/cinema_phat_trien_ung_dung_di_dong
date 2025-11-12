package com.example.datve;

public class Voucher {
    private String title;       // Tiêu đề voucher, ví dụ: "Giảm20% bắp nước"
    private String code;        // Mã voucher, ví dụ: "BAPNUOC20"
    private String expiryDate;  // Ngày hết hạn, ví dụ: "Hạn sử dụng: 31/12/2025"
    private int imageResId;     // ID của ảnh minh họa (lấy từ drawable)

    public Voucher(String title, String code, String expiryDate, int imageResId) {
        this.title = title;
        this.code = code;
        this.expiryDate = expiryDate;
        this.imageResId = imageResId;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getCode() {
        return code;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public int getImageResId() {
        return imageResId;
    }
}
