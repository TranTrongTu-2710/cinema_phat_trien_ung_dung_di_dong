package com.example.datve.seat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CinemaResponse {
    private String name;
    private int ticketPrice;
    private String city;
    private int seatsAvailable;
    private List<SeatGroup> seats;

    public CinemaResponse(JSONObject jsonObject) throws JSONException {
        this.name = jsonObject.getString("name");
        this.ticketPrice = jsonObject.getInt("ticketPrice");
        this.city = jsonObject.getString("city");
        this.seatsAvailable = jsonObject.getInt("seatsAvailable");

        this.seats = new ArrayList<>();
        JSONArray seatsArray = jsonObject.getJSONArray("seats");
        for (int i = 0; i < seatsArray.length(); i++) {
            JSONObject seatGroupObj = seatsArray.getJSONObject(i);
            seats.add(new SeatGroup(seatGroupObj));
        }
    }

    public static class SeatGroup {
        private String type;
        private List<String> listSeats;
        private int price;

        public SeatGroup(JSONObject jsonObject) throws JSONException {
            this.type = jsonObject.getString("type");
            this.price = jsonObject.getInt("price");

            this.listSeats = new ArrayList<>();
            JSONArray seatsArray = jsonObject.getJSONArray("listSeats");
            for (int i = 0; i < seatsArray.length(); i++) {
                listSeats.add(seatsArray.getString(i));
            }
        }

        public String getType() {
            return type;
        }

        public List<String> getListSeats() {
            return listSeats;
        }

        public int getPrice() {
            return price;
        }
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getTicketPrice() {
        return ticketPrice;
    }

    public String getCity() {
        return city;
    }

    public int getSeatsAvailable() {
        return seatsAvailable;
    }

    public List<SeatGroup> getSeats() {
        return seats;
    }
}