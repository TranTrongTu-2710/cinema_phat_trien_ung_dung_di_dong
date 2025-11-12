package com.example.datve;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.datve.user.SessionManager;
import com.example.datve.user.UserService;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private UserService userService;

    private static final String LOGIN_URL = "http://10.0.2.2:8080/users/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(getApplicationContext());
        userService = new UserService(this);

        // Kiểm tra nếu đã đăng nhập thì vào thẳng MainActivity
        if (sessionManager.isLoggedIn()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // Ánh xạ các view
        etUsername = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        progressBar = findViewById(R.id.progress_bar);

        // Sự kiện click cho nút "Đăng nhập"
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(username, password);
            }
        });

        // Sự kiện click cho text "Đăng ký ngay"
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser(String username, String password) {
        showLoading(true);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("username", username);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            showLoading(false);
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, LOGIN_URL, jsonBody,
                response -> {
                    showLoading(false);
                    try {
                        String token = response.getString("token");
                        JSONObject userObject = response.getJSONObject("user");
                        String userId = userObject.getString("_id");
                        String userUsername = userObject.getString("username");

                        // Lưu thông tin cơ bản
                        sessionManager.saveBasicUserInfo(token, userId, userUsername);

                        // Lấy thông tin đầy đủ từ API /users/me trong background
                        userService.fetchUserInfo(new com.example.datve.user.UserService.UserInfoCallback() {
                            @Override
                            public void onSuccess() {
                                runOnUiThread(() -> {
                                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                    navigateToMainActivity();
                                });
                            }

                            @Override
                            public void onError(String errorMessage) {
                                runOnUiThread(() -> {
                                    // Vẫn cho đăng nhập thành công dù không lấy được full info
                                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công! (Chưa tải đầy đủ thông tin)", Toast.LENGTH_SHORT).show();
                                    navigateToMainActivity();
                                });
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Lỗi xử lý dữ liệu từ server", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    showLoading(false);
                    if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                        Toast.makeText(LoginActivity.this, "Tên đăng nhập hoặc mật khẩu không đúng", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Lỗi kết nối hoặc server không phản hồi.", Toast.LENGTH_LONG).show();
                    }
                    error.printStackTrace();
                });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(android.view.View.VISIBLE);
            btnLogin.setEnabled(false);
        } else {
            progressBar.setVisibility(android.view.View.GONE);
            btnLogin.setEnabled(true);
        }
    }
}