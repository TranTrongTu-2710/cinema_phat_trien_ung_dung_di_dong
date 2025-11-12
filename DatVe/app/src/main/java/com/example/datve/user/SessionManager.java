package com.example.datve.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SessionManager {
    private static final String PREF_NAME = "AppSession";
    private static final String KEY_USER_TOKEN = "user_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_USERNAME = "user_username";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_RANK = "user_rank";
    private static final String KEY_USER_POINT = "user_point";
    private static final String KEY_USER_FULL_INFO = "user_full_info";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_USER_TOKEN, token);
        editor.apply();
    }

    public void saveUserId(String userId) {
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }

    public void saveUserName(String userName) {
        editor.putString(KEY_USER_NAME, userName);
        editor.apply();
    }

    public void saveUserPhone(String phone) {
        editor.putString(KEY_USER_PHONE, phone);
        editor.apply();
    }

    public void saveUserEmail(String email) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }

    public void saveUserUsername(String username) {
        editor.putString(KEY_USER_USERNAME, username);
        editor.apply();
    }

    public void saveUserRole(String role) {
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    public void saveUserRank(String rank) {
        editor.putString(KEY_USER_RANK, rank);
        editor.apply();
    }

    public void saveUserPoint(int point) {
        editor.putInt(KEY_USER_POINT, point);
        editor.apply();
    }

    public void setUserFullInfoLoaded(boolean loaded) {
        editor.putBoolean(KEY_USER_FULL_INFO, loaded);
        editor.apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, null);
    }

    public String getUserPhone() {
        return sharedPreferences.getString(KEY_USER_PHONE, null);
    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }

    public String getUserUsername() {
        return sharedPreferences.getString(KEY_USER_USERNAME, null);
    }

    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, null);
    }

    public String getUserRank() {
        return sharedPreferences.getString(KEY_USER_RANK, null);
    }

    public int getUserPoint() {
        return sharedPreferences.getInt(KEY_USER_POINT, 0);
    }

    public boolean isUserFullInfoLoaded() {
        return sharedPreferences.getBoolean(KEY_USER_FULL_INFO, false);
    }

    public String fetchAuthToken() {
        return sharedPreferences.getString(KEY_USER_TOKEN, null);
    }

    public void clearAuthToken() {
        editor.remove(KEY_USER_TOKEN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_PHONE);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_USERNAME);
        editor.remove(KEY_USER_ROLE);
        editor.remove(KEY_USER_RANK);
        editor.remove(KEY_USER_POINT);
        editor.remove(KEY_USER_FULL_INFO);
        editor.apply();
        Log.d("SessionManager", "Cleared all user data");
    }

    public boolean isLoggedIn() {
        return fetchAuthToken() != null;
    }

    public String getToken() {
        return fetchAuthToken();
    }

    public void saveBasicUserInfo(String token, String userId, String username) {
        editor.putString(KEY_USER_TOKEN, token);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_USERNAME, username);
        editor.apply();
        Log.d("SessionManager", "Saved basic user info: " + username);
    }

    public void saveFullUserInfo(String userId, String username, String name, String email, String phone, String role, String rank, int point) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_USERNAME, username);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHONE, phone);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_USER_RANK, rank);
        editor.putInt(KEY_USER_POINT, point);
        editor.putBoolean(KEY_USER_FULL_INFO, true);
        editor.apply();

        Log.d("SessionManager", "Saved full user info: " + name + " (" + username + ")");
    }

    public boolean needToFetchUserInfo() {
        return isLoggedIn() && !isUserFullInfoLoaded();
    }

    public String getDisplayInfo() {
        if (!isLoggedIn()) {
            return "Chưa đăng nhập";
        }

        StringBuilder sb = new StringBuilder();
        if (getUserName() != null) {
            sb.append(getUserName());
        }
        if (getUserPhone() != null) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(getUserPhone());
        }


        return sb.toString();
    }
}