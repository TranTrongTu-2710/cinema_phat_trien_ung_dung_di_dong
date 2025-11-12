package com.example.datve.user;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.datve.R;
import com.example.datve.user.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private Button btnConfirm;
    private SessionManager sessionManager;
    private RequestQueue requestQueue;

    private static final String CHANGE_PASSWORD_URL = "http://10.0.2.2:8080/users/me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        sessionManager = new SessionManager(this);
        requestQueue = Volley.newRequestQueue(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thay đổi mật khẩu");
        }

        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmNewPassword = findViewById(R.id.et_confirm_new_password);
        btnConfirm = findViewById(R.id.btn_confirm_change_password);

        btnConfirm.setOnClickListener(v -> handleChangePassword());
    }

    private void handleChangePassword() {
        String currentPass = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmNewPass = etConfirmNewPassword.getText().toString().trim();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmNewPass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmNewPass)) {
            Toast.makeText(this, "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            // Server của bạn có thể cần cả mật khẩu cũ và mới
            // Hãy kiểm tra lại yêu cầu của API. Ở đây tôi giả định server cần cả hai.
            // Nếu chỉ cần mật khẩu mới, hãy xóa dòng "currentPassword".
            requestBody.put("currentPassword", currentPass);
            requestBody.put("password", newPass);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PATCH, CHANGE_PASSWORD_URL, requestBody,
                response -> {
                    Toast.makeText(this, "Thay đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    // === PHẦN SỬA LỖI HIỂN THỊ THÔNG BÁO ===
                    String errorMessage = "Đã xảy ra lỗi không xác định."; // Tin nhắn mặc định

                    NetworkResponse networkResponse = error.networkResponse;
                    if (networkResponse != null && networkResponse.data != null) {
                        // Lấy chuỗi JSON lỗi từ server
                        String jsonError = new String(networkResponse.data, StandardCharsets.UTF_8);
                        Log.d("ChangePassError", "Error Body: " + jsonError); // In ra Logcat để debug

                        try {
                            // Chuyển chuỗi thành đối tượng JSON
                            JSONObject errorObject = new JSONObject(jsonError);
                            // Lấy giá trị của key "message"
                            errorMessage = errorObject.getString("message");
                        } catch (JSONException e) {
                            // Nếu không parse được JSON, có thể server trả về text thường
                            errorMessage = "Mật khẩu hiện tại không chính xác .";
                            Log.e("ChangePassError", "JSON parsing error: " + e.getMessage());
                        }
                    } else {
                        errorMessage = "Không thể kết nối đến server. Vui lòng kiểm tra lại mạng.";
                    }

                    Toast.makeText(ChangePasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    // === KẾT THÚC PHẦN SỬA LỖI ===
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String token = sessionManager.getToken();
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                }
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
