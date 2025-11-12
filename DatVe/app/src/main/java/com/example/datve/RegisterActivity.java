package com.example.datve;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginNow;
    private ProgressBar progressBar;

    // URL của API đăng ký.
    // LƯU Ý: Nếu dùng máy ảo Android, 10.0.2.2 sẽ trỏ đến localhost của máy tính.
    private static final String REGISTER_URL = "http://10.0.2.2:8080/users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ánh xạ các view từ layout
        etUsername = findViewById(R.id.et_register_username);
        etName = findViewById(R.id.et_register_name);
        etEmail = findViewById(R.id.et_register_email);
        etPhone = findViewById(R.id.et_register_phone);
        etPassword = findViewById(R.id.et_register_password);
        etConfirmPassword = findViewById(R.id.et_register_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLoginNow = findViewById(R.id.tv_login_now);
        progressBar = findViewById(R.id.register_progress_bar);

        // Xử lý sự kiện khi nhấn nút Đăng ký
        btnRegister.setOnClickListener(v -> handleRegistration());

        // Xử lý sự kiện khi nhấn "Đăng nhập ngay"
        tvLoginNow.setOnClickListener(v -> {
            // Quay trở lại màn hình đăng nhập, không cần tạo intent mới
            finish();
        });
    }

    private void handleRegistration() {
        // Lấy dữ liệu người dùng nhập
        String username = etUsername.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Kiểm tra dữ liệu đầu vào
        if (username.isEmpty() || name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ các trường bắt buộc (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Địa chỉ email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo đối tượng JSON để gửi đi
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("username", username);
            requestBody.put("name", name);
            requestBody.put("email", email);
            requestBody.put("phone", phone);
            requestBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        // Gọi API
        registerUser(requestBody);
    }

    private void registerUser(JSONObject requestBody) {
        showLoading(true);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, REGISTER_URL, requestBody,
                response -> {
                    // Xử lý khi API trả về thành công (status 2xx)
                    showLoading(false);
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_LONG).show();

                    // Chuyển người dùng về màn hình đăng nhập
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                },
                error -> {
                    // Xử lý khi có lỗi xảy ra
                    showLoading(false);
                    // Lỗi 409 Conflict thường là do username/email đã tồn tại
                    if (error.networkResponse != null && error.networkResponse.statusCode == 409) {
                        Toast.makeText(RegisterActivity.this, "Tên đăng nhập hoặc email đã tồn tại", Toast.LENGTH_LONG).show();
                    } else {
                        // Các lỗi khác
                        Toast.makeText(RegisterActivity.this, "Đã xảy ra lỗi. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                    }
                    error.printStackTrace();
                }
        );

        // Thêm request vào hàng đợi để thực thi
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
        }
    }
}
