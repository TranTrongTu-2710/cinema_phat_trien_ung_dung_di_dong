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
import com.android.volley.toolbox.JsonObjectRequest; // Thêm import này
import com.android.volley.toolbox.Volley;
import com.example.datve.R;
import com.example.datve.user.SessionManager;
import com.google.android.material.button.MaterialButton; // Thêm import này

import org.json.JSONException; // Thêm import này
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// === THAY ĐỔI: Implement listener của adapter ===
public class VoucherFragment extends Fragment implements VoucherAdapter.OnSaveVoucherListener {

    private RecyclerView recyclerViewVouchers;
    private ProgressBar progressBar;
    private VoucherAdapter voucherAdapter;
    private List<Voucher> voucherList;

    private SessionManager sessionManager;
    private RequestQueue requestQueue; // Thêm biến RequestQueue

    // === THÊM MỚI: URL để lưu voucher ===
    private static final String SAVE_USER_VOUCHER_URL = "http://10.0.2.2:8080/api/v1/user-voucher-details";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_voucher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext()); // Khởi tạo RequestQueue

        recyclerViewVouchers = view.findViewById(R.id.rv_vouchers);
        progressBar = view.findViewById(R.id.progressBar);

        voucherList = new ArrayList<>();
        // === THAY ĐỔI: Khởi tạo adapter với listener là chính fragment này ===
        voucherAdapter = new VoucherAdapter(getContext(), voucherList, this);
        recyclerViewVouchers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewVouchers.setAdapter(voucherAdapter);

        // Hàm này giữ nguyên logic
        fetchVouchers();
    }

    // === GIỮ NGUYÊN: Hàm này không thay đổi logic ===
    private void fetchVouchers() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xem voucher", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID người dùng.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://10.0.2.2:8080/api/v1/users/" + userId + "/unused-vouchers?onlyActive=true";

        progressBar.setVisibility(View.VISIBLE);
        recyclerViewVouchers.setVisibility(View.GONE);

        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    recyclerViewVouchers.setVisibility(View.VISIBLE);
                    voucherList.clear();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject voucherObject = response.getJSONObject(i);
                            voucherList.add(new Voucher(voucherObject));
                        }
                        voucherAdapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.e("VoucherFragment", "Lỗi parse JSON: " + e.getMessage());
                        Toast.makeText(getContext(), "Lỗi xử lý dữ liệu voucher.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("VoucherFragment", "Lỗi Volley: " + error.toString());
                    Toast.makeText(getContext(), "Không thể tải danh sách voucher.", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                if (sessionManager.isLoggedIn()) {
                    headers.put("Authorization", "Bearer " + sessionManager.getToken());
                }
                return headers;
            }
        };

        queue.add(jsonArrayRequest);
    }

    // === THÊM MỚI: Hàm xử lý logic khi nút "Lưu" được nhấn ===
    @Override
    public void onSaveVoucherClicked(Voucher voucher, MaterialButton button) {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để thực hiện", Toast.LENGTH_SHORT).show();
            return;
        }

        // Vô hiệu hóa nút để tránh nhấn nhiều lần
        button.setEnabled(false);
        button.setText("Đang lưu...");

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("userId", userId);
            requestBody.put("voucherId", voucher.getId());
            requestBody.put("status", "NOT_USED_YET");
        } catch (JSONException e) {
            e.printStackTrace();
            button.setEnabled(true); // Nếu lỗi thì bật lại nút
            button.setText("Lưu");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SAVE_USER_VOUCHER_URL, requestBody,
                response -> {
                    // Xử lý khi API trả về mã 2xx (thành công)
                    Toast.makeText(getContext(), "Lưu voucher thành công!", Toast.LENGTH_SHORT).show();
                    button.setText("Đã lưu");
                    // Không cần làm gì thêm, chỉ cần giữ trạng thái nút
                },
                error -> {
                    // Xử lý khi có lỗi
                    if (error.networkResponse != null && error.networkResponse.statusCode == 409) {
                        // Mã 409 Conflict: Voucher đã tồn tại
                        Toast.makeText(getContext(), "Voucher này đã có trong ví của bạn!", Toast.LENGTH_SHORT).show();
                        button.setText("Đã có");
                    } else {
                        // Các lỗi khác
                        Toast.makeText(getContext(), "Có lỗi xảy ra, không thể lưu voucher.", Toast.LENGTH_SHORT).show();
                        button.setEnabled(true); // Bật lại nút nếu lỗi
                        button.setText("Lưu");
                    }
                    Log.e("SaveVoucher", "Lỗi: " + error.toString());
                }) {
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

        requestQueue.add(request);
    }
}
