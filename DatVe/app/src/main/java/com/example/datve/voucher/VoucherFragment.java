package com.example.datve.voucher;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.datve.R;
import com.example.datve.user.SessionManager; // Giả sử bạn có SessionManager

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoucherFragment extends Fragment {

    private RecyclerView recyclerViewVouchers;
    private ProgressBar progressBar; // Thêm ProgressBar để báo đang tải
    private VoucherAdapter voucherAdapter;
    private List<Voucher> voucherList;

    private SessionManager sessionManager; // Để lấy thông tin người dùng

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_voucher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(requireContext());

        // Ánh xạ các view
        recyclerViewVouchers = view.findViewById(R.id.rv_vouchers);
        progressBar = view.findViewById(R.id.progressBar); // Giả sử bạn có ProgressBar trong layout

        // Thiết lập RecyclerView
        voucherList = new ArrayList<>();
        voucherAdapter = new VoucherAdapter(getContext(), voucherList);
        recyclerViewVouchers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewVouchers.setAdapter(voucherAdapter);

        // Gọi API để lấy dữ liệu
        fetchVouchers();
    }

    /**
     * Phương thức mới để gọi API lấy danh sách voucher.
     */
    private void fetchVouchers() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xem voucher", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId(); // Giả sử SessionManager có hàm này
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID người dùng.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Thay localhost bằng 10.0.2.2 khi chạy trên máy ảo Android
        String url = "http://10.0.2.2:8080/api/v1/users/" + userId + "/unused-vouchers?onlyActive=true";

        // Hiển thị ProgressBar
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewVouchers.setVisibility(View.GONE);

        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    // Xử lý khi nhận được phản hồi thành công
                    progressBar.setVisibility(View.GONE);
                    recyclerViewVouchers.setVisibility(View.VISIBLE);
                    voucherList.clear(); // Xóa dữ liệu cũ
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject voucherObject = response.getJSONObject(i);
                            voucherList.add(new Voucher(voucherObject));
                        }
                        voucherAdapter.notifyDataSetChanged(); // Cập nhật RecyclerView
                    } catch (Exception e) {
                        Log.e("VoucherFragment", "Lỗi parse JSON: " + e.getMessage());
                        Toast.makeText(getContext(), "Lỗi xử lý dữ liệu voucher.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Xử lý khi có lỗi
                    progressBar.setVisibility(View.GONE);
                    Log.e("VoucherFragment", "Lỗi Volley: " + error.toString());
                    Toast.makeText(getContext(), "Không thể tải danh sách voucher.", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                // Thêm token xác thực nếu API yêu cầu
                Map<String, String> headers = new HashMap<>();
                if (sessionManager.isLoggedIn()) {
                    headers.put("Authorization", "Bearer " + sessionManager.getToken());
                }
                return headers;
            }
        };

        queue.add(jsonArrayRequest);
    }
}
