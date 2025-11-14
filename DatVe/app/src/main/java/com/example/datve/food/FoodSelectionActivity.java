package com.example.datve.food;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.datve.MainActivity;
import com.example.datve.R;
import com.example.datve.user.SessionManager;
import com.example.datve.voucher.Voucher;
import com.example.datve.voucher.VoucherSelectionBottomSheet;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FoodSelectionActivity extends AppCompatActivity implements
        FoodAdapter.OnQuantityChangedListener, VoucherSelectionBottomSheet.VoucherSelectionListener {

    // ... (Các hằng số và khai báo biến giữ nguyên) ...
    public static final String EXTRA_SELECTED_SEATS = "SELECTED_SEATS";
    public static final String EXTRA_TOTAL_SEAT_PRICE = "TOTAL_SEAT_PRICE";
    public static final String EXTRA_SHOWTIME_ID = "SHOWTIME_ID";
    public static final String EXTRA_SELECTED_DATE = "SELECTED_DATE";
    public static final String EXTRA_START_TIME = "START_TIME";
    public static final String EXTRA_MOVIE_TITLE = "MOVIE_TITLE";

    private List<String> selectedSeats;
    private int totalSeatPrice;
    private String showtimeId, selectedDate, startTime, movieTitle;

    // UI Views
    private RecyclerView recyclerViewFoods;
    private TextView tvSelectedSeats, tvSeatPrice, tvFoodPrice, tvTotalPrice, tvLoginStatus;
    private MaterialCardView cardLoginStatus;
    private Button btnContinue;

    // Voucher UI Views
    private TextView tvSelectVoucher, tvVoucherDiscount, tvAppliedVoucherName;
    private LinearLayout layoutVoucherDiscount;
    private RelativeLayout layoutAppliedVoucher;
    private ImageView ivRemoveVoucher;

    // Data
    private List<Food> foods = new ArrayList<>();
    private FoodAdapter foodAdapter;
    private int totalFoodPrice = 0;

    private List<Voucher> availableVouchers = new ArrayList<>();
    private Voucher selectedVoucher = null;
    private int discountAmount = 0;

    private SessionManager sessionManager;

    // URLs
    private static final String FOOD_BASE_URL = "http://10.0.2.2:8080/foods";
    private static final String RESERVATION_URL = "http://10.0.2.2:8080/reservations";
    private static final String VOUCHER_BASE_URL = "http://10.0.2.2:8080/api/v1/users/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_selection);

        sessionManager = new SessionManager(this);

        if (!getIntentData()) {
            return;
        }

        setupUI();
        fetchFoods();
        fetchVouchers();
        handleOnBackPressed();
    }

    private boolean getIntentData() {
        selectedSeats = getIntent().getStringArrayListExtra(EXTRA_SELECTED_SEATS);
        totalSeatPrice = getIntent().getIntExtra(EXTRA_TOTAL_SEAT_PRICE, 0);
        showtimeId = getIntent().getStringExtra(EXTRA_SHOWTIME_ID);
        selectedDate = getIntent().getStringExtra(EXTRA_SELECTED_DATE);
        startTime = getIntent().getStringExtra(EXTRA_START_TIME);

        // Sửa lỗi: Thêm kiểm tra movieTitle
        if (selectedSeats == null || selectedSeats.isEmpty() || showtimeId == null || selectedDate == null ) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin đặt vé.", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        return true;
    }

    private void setupUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(movieTitle);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Ánh xạ các view cũ
        recyclerViewFoods = findViewById(R.id.recycler_view_foods);
        tvSelectedSeats = findViewById(R.id.tv_selected_seats);
        tvSeatPrice = findViewById(R.id.tv_seat_price);
        tvFoodPrice = findViewById(R.id.tv_food_price);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        tvLoginStatus = findViewById(R.id.tv_login_status);
        cardLoginStatus = findViewById(R.id.card_login_status);
        btnContinue = findViewById(R.id.btn_continue);

        // Ánh xạ các view cho voucher
        tvSelectVoucher = findViewById(R.id.tv_select_voucher);
        tvVoucherDiscount = findViewById(R.id.tv_voucher_discount);
        layoutVoucherDiscount = findViewById(R.id.layout_voucher_discount);
        layoutAppliedVoucher = findViewById(R.id.layout_applied_voucher);
        tvAppliedVoucherName = findViewById(R.id.tv_applied_voucher_name);
        ivRemoveVoucher = findViewById(R.id.iv_remove_voucher);

        tvSelectedSeats.setText("Ghế: " + String.join(", ", selectedSeats));
        updateLoginStatus();
        updatePrices();

        btnContinue.setOnClickListener(v -> createReservationWithUserInfo());
        tvSelectVoucher.setOnClickListener(v -> showVoucherSelectionDialog());
        ivRemoveVoucher.setOnClickListener(v -> removeVoucher());

        foodAdapter = new FoodAdapter(foods, this);
        recyclerViewFoods.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFoods.setAdapter(foodAdapter);
    }

    // ... (Các hàm fetchFoods, fetchVouchers, showVoucherSelectionDialog, onVoucherSelected, removeVoucher giữ nguyên) ...

    private void fetchFoods() {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, FOOD_BASE_URL, null,
                response -> {
                    try {
                        foods.clear();
                        for (int i = 0; i < response.length(); i++) {
                            foods.add(new Food(response.getJSONObject(i)));
                        }
                        foodAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Toast.makeText(this, "Lỗi xử lý dữ liệu đồ ăn", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Không thể tải danh sách đồ ăn", Toast.LENGTH_SHORT).show();
                });
        queue.add(request);
    }

    private void fetchVouchers() {
        if (!sessionManager.isLoggedIn() || sessionManager.getUserId() == null) {
            return;
        }
        String userId = sessionManager.getUserId();
        String url = VOUCHER_BASE_URL + userId + "/unused-vouchers?onlyActive=true";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    availableVouchers.clear();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            availableVouchers.add(new Voucher(response.getJSONObject(i)));
                        }
                    } catch (JSONException e) {
                        Log.e("Voucher", "Error parsing vouchers", e);
                    }
                },
                error -> Log.e("Voucher", "Error fetching vouchers", error)
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void showVoucherSelectionDialog() {
        if (availableVouchers.isEmpty()){
            Toast.makeText(this, "Bạn không có voucher nào.", Toast.LENGTH_SHORT).show();
            return;
        }
        int currentTotal = totalSeatPrice + totalFoodPrice;
        VoucherSelectionBottomSheet bottomSheet = VoucherSelectionBottomSheet.newInstance(new ArrayList<>(availableVouchers), currentTotal);
        bottomSheet.show(getSupportFragmentManager(), "VoucherBottomSheet");
    }

    @Override
    public void onVoucherSelected(Voucher voucher) {
        this.selectedVoucher = voucher;
        updatePrices();
    }

    private void removeVoucher() {
        this.selectedVoucher = null;
        updatePrices();
    }

    private void updatePrices() {
        int originalTotal = totalSeatPrice + totalFoodPrice;

        if (selectedVoucher != null) {
            if (originalTotal < selectedVoucher.getMinOrderTotal()) {
                Toast.makeText(this, "Voucher không còn hợp lệ, đã tự động hủy.", Toast.LENGTH_SHORT).show();
                removeVoucher();
                return;
            }
            discountAmount = selectedVoucher.calculateDiscount(originalTotal);
        } else {
            discountAmount = 0;
        }

        int finalTotal = originalTotal - discountAmount;

        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        tvSeatPrice.setText(format.format(totalSeatPrice) + "đ");
        tvFoodPrice.setText(format.format(totalFoodPrice) + "đ");
        tvTotalPrice.setText(format.format(finalTotal) + "đ");

        if (discountAmount > 0 && selectedVoucher != null) {
            tvVoucherDiscount.setText("-" + format.format(discountAmount) + "đ");
            layoutVoucherDiscount.setVisibility(View.VISIBLE);
            tvAppliedVoucherName.setText("Đã áp dụng: " + selectedVoucher.getName());
            layoutAppliedVoucher.setVisibility(View.VISIBLE);
            tvSelectVoucher.setVisibility(View.GONE);
        } else {
            layoutVoucherDiscount.setVisibility(View.GONE);
            layoutAppliedVoucher.setVisibility(View.GONE);
            tvSelectVoucher.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onQuantityChanged() {
        totalFoodPrice = 0;
        for (Food f : foods) {
            totalFoodPrice += f.getTotalPrice();
        }
        updatePrices();
    }

    private void createReservationWithUserInfo() {
        if (!sessionManager.isLoggedIn()) {
            showUserInfoDialog();
            return;
        }
        String username = sessionManager.getUserUsername();
        String phone = sessionManager.getUserPhone();

        if (username == null || phone == null) {
            Toast.makeText(this, "Thông tin người dùng không đầy đủ.", Toast.LENGTH_SHORT).show();
            return;
        }
        createReservation(username, phone);
    }

    private void createReservation(String username, String phone) {
        RequestQueue queue = Volley.newRequestQueue(this);
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("seats", new JSONArray(selectedSeats));
            requestBody.put("food", getSelectedFoodsAsJsonArray());
            requestBody.put("checkin", false);
            requestBody.put("showtimeId", showtimeId);
            requestBody.put("date", selectedDate + "T00:00:00.000Z");
            requestBody.put("startAt", startTime);
            requestBody.put("username", username);
            requestBody.put("phone", phone);

            // === THAY ĐỔI THEO YÊU CẦU ===
            // Chỉ thêm `userId` và `voucherId` nếu có voucher được áp dụng thành công
            if (selectedVoucher != null && discountAmount > 0) {
                requestBody.put("voucherId", selectedVoucher.getId());
                // Chỉ gửi userId khi có voucher được chọn
                if (sessionManager.isLoggedIn() && sessionManager.getUserId() != null) {
                    requestBody.put("userId", sessionManager.getUserId());
                }
            }
            // Nếu không có voucher, 'userId' sẽ không được gửi đi

            int finalTotal = totalSeatPrice + totalFoodPrice - discountAmount;
            requestBody.put("total", finalTotal);

            Log.d("Reservation", "Sending request: " + requestBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, RESERVATION_URL, requestBody,
                    response -> {
                        Log.d("Reservation", "Success: " + response.toString());
                        showSuccessDialog(username, phone);
                    },
                    error -> {
                        Log.e("Reservation", "Error: " + error.toString());
                        handleReservationError(error);
                    });
            queue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo dữ liệu đặt vé", Toast.LENGTH_SHORT).show();
        }
    }

    private JSONArray getSelectedFoodsAsJsonArray() {
        JSONArray foodArray = new JSONArray();
        for (Food food : foods) {
            if (food.getQuantity() > 0) {
                try {
                    JSONObject foodObject = new JSONObject();
                    foodObject.put("foodId", food.getId());
                    foodObject.put("foodName", food.getName());
                    foodObject.put("price", food.getPrice());
                    foodObject.put("quantity", food.getQuantity());
                    foodArray.put(foodObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return foodArray;
    }

    private void showSuccessDialog(String username, String phone) {
        StringBuilder message = new StringBuilder();
        message.append("Cảm ơn ").append(username).append("!\n\n");
        message.append("📞 SĐT: ").append(phone).append("\n");
        message.append("🎬 Ghế: ").append(String.join(", ", selectedSeats)).append("\n");

        if (discountAmount > 0 && selectedVoucher != null) {
            message.append("🏷️ Voucher: ").append(selectedVoucher.getName()).append("\n");
        }

        int finalTotal = totalSeatPrice + totalFoodPrice - discountAmount;
        message.append("💰 Tổng tiền: ").append(NumberFormat.getInstance(new Locale("vi", "VN")).format(finalTotal)).append("đ\n\n");
        message.append("Vui lòng đến rạp trước 15 phút để check-in.");

        new AlertDialog.Builder(this)
                .setTitle("Đặt vé thành công!")
                .setMessage(message.toString())
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(FoodSelectionActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void handleOnBackPressed() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(FoodSelectionActivity.this)
                        .setTitle("Xác nhận")
                        .setMessage("Bạn có muốn hủy đặt vé?")
                        .setPositiveButton("Có", (dialog, which) -> {
                            setResult(RESULT_CANCELED);
                            finish();
                        })
                        .setNegativeButton("Không", null)
                        .show();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void updateLoginStatus() {
        if (sessionManager.isLoggedIn()) {
            String displayInfo = sessionManager.getDisplayInfo();
            if (sessionManager.isUserFullInfoLoaded()) {
                tvLoginStatus.setText("Đặt vé với: " + displayInfo);
                btnContinue.setText("Đặt vé");
            } else {
                String username = sessionManager.getUserUsername();
                tvLoginStatus.setText(username != null ? "Đang tải thông tin... (" + username + ")" : "Đang tải thông tin...");
                btnContinue.setText("Đặt vé");
            }
        } else {
            tvLoginStatus.setText("Bạn đang đặt vé với tư cách khách");
            btnContinue.setText("Đặt vé");
        }
        cardLoginStatus.setVisibility(View.VISIBLE);
    }

    private void showUserInfoDialog() {
        Toast.makeText(this, "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_SHORT).show();
    }

    private void handleReservationError(com.android.volley.VolleyError error) {
        String errorMsg = "Đặt vé thất bại. Vui lòng thử lại sau.";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            String responseBody = new String(error.networkResponse.data);
            try {
                JSONObject obj = new JSONObject(responseBody);
                errorMsg = obj.optString("message", errorMsg);
            } catch (JSONException e) {
                // Ignore
            }
        }
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    }
}
