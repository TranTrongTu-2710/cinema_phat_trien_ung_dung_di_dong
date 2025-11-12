package com.example.datve.showtime;

import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Showtime {
    private final String id;
    private final String startAt;     // Ví dụ: "20:30"
    private final String startDate;   // Ví dụ: "2025-11-15T00:00:00.000Z"
    private final String cinemaName;  // Ví dụ: "Vĩnh Yên 1"
    private final String cinemaId;    // Ví dụ: "690d41665e56a211182791a4"

    public Showtime(JSONObject jsonObject) throws JSONException {
        this.id = jsonObject.getString("_id");
        this.startAt = jsonObject.getString("startAt");
        this.startDate = jsonObject.getString("startDate");
        this.cinemaName = jsonObject.optString("name", "Không rõ rạp");
        this.cinemaId = jsonObject.getString("cinemaId");
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getStartAt() {
        return startAt;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public String getCinemaId() {
        return cinemaId;
    }

    // Hàm tiện ích để lấy đối tượng Date từ chuỗi startDate (UTC)
    public Date getStartDateObject() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(startDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}