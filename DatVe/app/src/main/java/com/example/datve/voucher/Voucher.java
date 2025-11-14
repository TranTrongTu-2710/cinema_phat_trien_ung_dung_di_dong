package com.example.datve.voucher;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Voucher implements Serializable { // Implement Serializable để có thể truyền giữa các Activity/Fragment
    private String id;
    private String code;
    private String name;
    private String description;
    private String type; // PERCENT or FIXED
    private double value;
    private int maxDiscount;
    private int minOrderTotal;
    private String endAt;

    public Voucher(JSONObject jsonObject) throws JSONException {
        this.id = jsonObject.getString("id");
        this.code = jsonObject.getString("code");
        this.name = jsonObject.getString("name");
        this.description = jsonObject.getString("description");
        this.type = jsonObject.getString("type");
        this.value = jsonObject.getDouble("value");
        this.maxDiscount = jsonObject.optInt("maxDiscount", 0);
        this.minOrderTotal = jsonObject.optInt("minOrderTotal", 0);
        this.endAt = jsonObject.getString("endAt");
    }

    // Getters
    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public double getValue() { return value; }
    public int getMaxDiscount() { return maxDiscount; }
    public int getMinOrderTotal() { return minOrderTotal; }
    public String getEndAt() { return endAt; }

    /**
     * Tính toán số tiền được giảm dựa trên tổng tiền đơn hàng.
     * @param originalTotal Tổng tiền gốc.
     * @return Số tiền được giảm.
     */
    public int calculateDiscount(int originalTotal) {
        if (originalTotal < this.minOrderTotal) {
            return 0; // Không đủ điều kiện
        }

        int discountAmount = 0;
        if ("PERCENT".equalsIgnoreCase(this.type)) {
            discountAmount = (int) (originalTotal * (this.value / 100.0));
            if (this.maxDiscount > 0 && discountAmount > this.maxDiscount) {
                discountAmount = this.maxDiscount;
            }
        } else { // Giả sử type còn lại là FIXED
            discountAmount = (int) this.value;
        }
        return discountAmount;
    }
}
