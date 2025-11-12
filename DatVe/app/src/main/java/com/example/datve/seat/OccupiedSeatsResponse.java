package com.example.datve.seat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OccupiedSeatsResponse {
    private String showtimeId;
    private String date;
    private String startAt;
    private List<String> occupied;

    public OccupiedSeatsResponse(JSONObject jsonObject) throws JSONException {
        this.showtimeId = jsonObject.getString("showtimeId");
        this.date = jsonObject.getString("date");
        this.startAt = jsonObject.getString("startAt");

        this.occupied = new ArrayList<>();
        JSONArray occupiedArray = jsonObject.getJSONArray("occupied");
        for (int i = 0; i < occupiedArray.length(); i++) {
            occupied.add(occupiedArray.getString(i));
        }
    }

    // Getters
    public String getShowtimeId() {
        return showtimeId;
    }

    public String getDate() {
        return date;
    }

    public String getStartAt() {
        return startAt;
    }

    public List<String> getOccupied() {
        return occupied;
    }
}