package com.example.datve.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.datve.LoginActivity;
import com.example.datve.R;

import org.json.JSONException;

import java.text.NumberFormat;
import java.util.Locale;

public class AccountFragment extends Fragment {

    private TextView tvUserName, tvUserPhone, tvMembershipTier, tvRewardPoints;
    private TextView btnChangePassword, btnLogout;

    private SessionManager sessionManager;

    // URL để lấy thông tin người dùng. "me" thường được dùng để chỉ người dùng hiện tại dựa trên token.
    private static final String USER_INFO_URL = "http://10.0.2.2:8080/users/me";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(requireContext());

        // Ánh xạ views từ layout
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserPhone = view.findViewById(R.id.tv_user_phone);
        tvMembershipTier = view.findViewById(R.id.tv_membership_tier);
        tvRewardPoints = view.findViewById(R.id.tv_reward_points);
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        btnLogout = view.findViewById(R.id.btn_logout);

        // Gọi API để lấy thông tin người dùng
        fetchUserData();

        // Sự kiện cho nút Đăng xuất
        btnLogout.setOnClickListener(v -> {
            // Xóa token đã lưu
            sessionManager.clearAuthToken();
            Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();

            // Chuyển về màn hình Login và xóa hết các màn hình cũ
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        // Sự kiện cho nút Thay đổi mật khẩu
        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Hàm gọi API để lấy thông tin chi tiết của người dùng đang đăng nhập.
     */
    private void fetchUserData() {
        // Tạo hàng đợi yêu cầu Volley
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        // Tạo một AuthRequest (lớp custom của chúng ta) để tự động thêm token vào header
        AuthRequest authRequest = new AuthRequest(Request.Method.GET, USER_INFO_URL, sessionManager, null,
                response -> {
                    // Xử lý khi API trả về thành công
                    try {
                        // Lấy dữ liệu từ JSON object trả về
                        // Các key "name", "phone",... phải khớp với key trong JSON server trả về
                        String name = response.getString("name");
                        String phone = response.getString("phone");
                        // Ví dụ lấy các thông tin khác, có thể có hoặc không
                        String membership = response.optString("rank"); // "Thường" là giá trị mặc định nếu không có
                        int points = response.optInt("point"); // 0 là giá trị mặc định
                        tvUserName.setText(name);
                        tvUserPhone.setText(phone);
                        tvMembershipTier.setText(membership);
                        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                        tvRewardPoints.setText(numberFormat.format(points));

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Lỗi định dạng dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Xử lý khi có lỗi
                    if (error.networkResponse != null && error.networkResponse.statusCode == 403) {
                        // Lỗi 403 Forbidden hoặc 401 Unauthorized thường do token không hợp lệ/hết hạn
                        Toast.makeText(getContext(), "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                        // Tự động đăng xuất người dùng
                        btnLogout.performClick();
                    } else {
                        Toast.makeText(getContext(), "Không thể tải thông tin tài khoản", Toast.LENGTH_SHORT).show();
                    }
                    error.printStackTrace();
                }
        );

        // Thêm request vào hàng đợi để thực thi
        queue.add(authRequest);
    }
}
