package com.example.datve.user;

public class User {
    private String name;
    private String phone;
    private String membershipTier;
    private int rewardPoints;

    // Constructor để tạo dữ liệu mẫu
    public User(String name, String phone, String membershipTier, int rewardPoints) {
        this.name = name;
        this.phone = phone;
        this.membershipTier = membershipTier;
        this.rewardPoints = rewardPoints;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getMembershipTier() {
        return membershipTier;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }
}
