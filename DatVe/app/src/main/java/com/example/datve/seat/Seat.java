package com.example.datve.seat;

public class Seat {
    private String seatNumber; // Ví dụ: "A1", "B2"
    private SeatType type; // REG hoặc VIP
    private SeatStatus status; // AVAILABLE, SELECTED, OCCUPIED
    private int price;

    public enum SeatType {
        REG, VIP
    }

    public enum SeatStatus {
        AVAILABLE,   // Ghế trống
        SELECTED,    // Đang chọn
        OCCUPIED     // Đã bán
    }

    public Seat(String seatNumber, SeatType type, int price) {
        this.seatNumber = seatNumber;
        this.type = type;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    // Getters and Setters
    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatType getType() {
        return type;
    }

    public void setType(SeatType type) {
        this.type = type;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    // Lấy hàng ghế (A, B, C...)
    public char getRow() {
        return seatNumber.charAt(0);
    }

    // Lấy số ghế (1, 2, 3...)
    public String getColumn() {
        return seatNumber.substring(1);
    }

    @Override
    public String toString() {
        return seatNumber;
    }
}