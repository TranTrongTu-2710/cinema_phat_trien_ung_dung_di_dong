package com.example.datve.user;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class UserService {
    private static final String TAG = "UserService";
    private static final String USER_INFO_URL = "http://10.0.2.2:8080/users/me";

    private Context context;
    private SessionManager sessionManager;
    private RequestQueue requestQueue;

    public UserService(Context context) {
        this.context = context;
        this.sessionManager = new SessionManager(context);
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public void fetchUserInfoIfNeeded() {
        if (!sessionManager.needToFetchUserInfo()) {
            Log.d(TAG, "User info already loaded or user not logged in");
            return;
        }

        String token = sessionManager.getToken();
        if (token == null) {
            Log.w(TAG, "No token available for fetching user info");
            return;
        }

        Log.d(TAG, "Fetching user info from server...");

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                USER_INFO_URL,
                null,
                response -> {
                    Log.d(TAG, "User info response received");
                    handleUserInfoResponse(response);
                },
                error -> {
                    Log.e(TAG, "Error fetching user info: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Error code: " + error.networkResponse.statusCode);
                    }
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    public void fetchUserInfo(UserInfoCallback callback) {
        String token = sessionManager.getToken();
        if (token == null) {
            Log.w(TAG, "No token available for fetching user info");
            if (callback != null) {
                callback.onError("No token available");
            }
            return;
        }

        Log.d(TAG, "Fetching user info from server...");

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                USER_INFO_URL,
                null,
                response -> {
                    Log.d(TAG, "User info response received");
                    boolean success = handleUserInfoResponse(response);
                    if (callback != null) {
                        if (success) {
                            callback.onSuccess();
                        } else {
                            callback.onError("Failed to parse user info");
                        }
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching user info: " + error.toString());
                    if (callback != null) {
                        String errorMsg = "Lỗi kết nối";
                        if (error.networkResponse != null) {
                            if (error.networkResponse.statusCode == 401) {
                                errorMsg = "Token không hợp lệ";
                            } else if (error.networkResponse.statusCode == 404) {
                                errorMsg = "Không tìm thấy thông tin user";
                            }
                        }
                        callback.onError(errorMsg);
                    }
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private boolean handleUserInfoResponse(JSONObject response) {
        try {
            // Parse dữ liệu theo JSON structure thực tế
            String userId = response.getString("_id");
            String username = response.getString("username");
            String name = response.getString("name");
            String email = response.getString("email");
            String phone = response.getString("phone");
            String role = response.getString("role");
            String rank = response.getString("rank");
            int point = response.getInt("point");

            // Lưu thông tin đầy đủ vào SessionManager
            sessionManager.saveFullUserInfo(userId, username, name, email, phone, role, rank, point);

            Log.d(TAG, "User info saved successfully: " + name + " (" + username + ")");
            return true;

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing user info: " + e.getMessage());
            Log.e(TAG, "Response: " + response.toString());
            return false;
        }
    }

    public void refreshUserInfo(UserInfoCallback callback) {
        sessionManager.setUserFullInfoLoaded(false);
        fetchUserInfo(callback);
    }

    public interface UserInfoCallback {
        void onSuccess();
        void onError(String errorMessage);
    }
}