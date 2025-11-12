package com.example.datve.notification;

import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class Notification {
    private String id;
    private String title;
    private String description;
    private String date;
    private int active;
    private boolean isRead = false;

    public Notification(JSONObject jsonObject) throws JSONException {
        this.id = jsonObject.getString("_id");
        this.title = jsonObject.getString("title");
        this.description = jsonObject.getString("description");
        this.date = jsonObject.getString("date");
        this.active = jsonObject.getInt("active");
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return description; }
    public int getActive() { return active; }

    public String getTimestamp() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date notificationDate = sdf.parse(this.date);
            long diffInMillis = new Date().getTime() - notificationDate.getTime();

            long seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis);
            if (seconds < 60) return seconds + " giây trước";
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
            if (minutes < 60) return minutes + " phút trước";
            long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
            if (hours < 24) return hours + " giờ trước";
            long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            return days + " ngày trước";
        } catch (ParseException e) {
            e.printStackTrace();
            return "Vừa xong";
        }
    }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
