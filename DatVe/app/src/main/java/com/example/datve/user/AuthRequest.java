package com.example.datve.user;

import androidx.annotation.Nullable;
import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class AuthRequest extends JsonObjectRequest {
    private SessionManager sessionManager;

    public AuthRequest(int method, String url, SessionManager sessionManager, @Nullable JSONObject jsonRequest, Response.Listener<JSONObject> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
        this.sessionManager = sessionManager;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>();
        String token = sessionManager.fetchAuthToken();
        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }
        // Thêm các header mặc định khác nếu cần
        headers.put("Content-Type", "application/json; charset=utf-8");
        return headers;
    }
}
