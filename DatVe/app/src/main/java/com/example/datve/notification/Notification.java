package com.example.datve.notification;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class Notification {
    private String id;
    private String title;
    private String description;
    private Date date;
    private int active;
    private boolean isRead = false;

    public Notification(JSONObject jsonObject) throws JSONException {
        this.id = jsonObject.optString("_id");
        this.title = jsonObject.optString("title");
        this.description = jsonObject.optString("description");
        this.active = jsonObject.optInt("active");

        if (jsonObject.has("date")) {
            String dateValue = jsonObject.getString("date");
            Log.d("NotificationParse", "Raw date string from JSON: " + dateValue);
            this.date = parseDateString(dateValue);
            if (this.date == null) {
                Log.e("NotificationParse", "Failed to parse date string: " + dateValue);
            }
        }
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return description; }
    public int getActive() { return active; }

    public String getTimestamp() {
        if (this.date == null) {
            return "";
        }
        return calculateTimeAgo(this.date);
    }

    /**
     * Hàm helper để parse chuỗi ngày tháng từ server.
     * Đã được cập nhật để xử lý múi giờ +HH:mm.
     */
    private Date parseDateString(String dateString) {
        // === SỬA LỖI Ở ĐÂY ===
        // Ưu tiên 1: Định dạng ISO 8601 với múi giờ +HH:mm (khớp với logcat)
        // Dùng XXX để parse múi giờ dạng +00:00
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
            return sdf.parse(dateString);
        } catch (ParseException e) {
            // Thử định dạng tiếp theo
        }

        // Ưu tiên 2: Định dạng ISO 8601 với Z (phòng trường hợp API thay đổi)
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(dateString);
        } catch (ParseException ex) {
            // Thử định dạng tiếp theo
        }

        // Ưu tiên 3: Định dạng "yyyy-MM-dd HH:mm:ss"
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.parse(dateString);
        } catch (ParseException e) {
            // Thử định dạng tiếp theo
        }

        // Ưu tiên 4: Số epoch milliseconds
        try {
            long epochMillis = Long.parseLong(dateString);
            return new Date(epochMillis);
        } catch (NumberFormatException e) {
            // Tất cả đều thất bại
        }

        return null; // Trả về null nếu không có định dạng nào khớp
    }


    /**
     * Tách logic tính toán ra một hàm riêng cho dễ đọc và tái sử dụng
     */
    private String calculateTimeAgo(Date notificationDate) {
        if (notificationDate == null) return "";

        long diffInMillis = new Date().getTime() - notificationDate.getTime();

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis);
        if (seconds < 60) {
            return "Vừa xong";
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
        if (minutes < 60) {
            return minutes + " phút trước";
        }

        long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
        if (hours < 24) {
            return hours + " giờ trước";
        }

        long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        return days + " ngày trước";
    }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { this.isRead = read; }
}

