package com.example.datve.food;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Food {
    private String id;
    private String name;
    private int price;
    private String description;
    private String image;
    private List<PathItem> pathItems;
    private int quantity;
    //
    public Food(JSONObject jsonObject) throws JSONException {
        this.id = jsonObject.getString("_id");
        this.name = jsonObject.getString("name");
        this.price = jsonObject.getInt("price");
        this.description = jsonObject.optString("description", "");
        this.image = jsonObject.optString("image", "");
        this.quantity = 0;

        this.pathItems = new ArrayList<>();
        if (jsonObject.has("pathItem") && !jsonObject.isNull("pathItem")) {
            JSONArray pathItemArray = jsonObject.getJSONArray("pathItem");
            for (int i = 0; i < pathItemArray.length(); i++) {
                JSONObject pathItemObj = pathItemArray.getJSONObject(i);
                pathItems.add(new PathItem(
                        pathItemObj.getString("item"),
                        pathItemObj.getInt("price"),
                        pathItemObj.getInt("quantity")
                ));
            }
        }
    }

    public static class PathItem {
        private String item;
        private int price;
        private int quantity;

        public PathItem(String item, int price, int quantity) {
            this.item = item;
            this.price = price;
            this.quantity = quantity;
        }

        // Getters
        public String getItem() { return item; }
        public int getPrice() { return price; }
        public int getQuantity() { return quantity; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public String getDescription() { return description; }
    public String getImage() { return image; }
    public List<PathItem> getPathItems() { return pathItems; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getTotalPrice() {
        return price * quantity;
    }
}