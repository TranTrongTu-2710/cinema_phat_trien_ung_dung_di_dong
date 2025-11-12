package com.example.datve.food;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.datve.MainActivity;
import com.example.datve.R;
import com.example.datve.user.SessionManager;
import com.example.datve.user.UserService;
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

// Sửa lại interface cho khớp
public class FoodSelectionActivity extends AppCompatActivity implements FoodAdapter.OnQuantityChangedListener {

    public static final String EXTRA_SELECTED_SEATS = "SELECTED_SEATS";
    public static final String EXTRA_TOTAL_SEAT_PRICE = "TOTAL_SEAT_PRICE";
    public static final String EXTRA_SHOWTIME_ID = "SHOWTIME_ID";
    public static final String EXTRA_SELECTED_DATE = "SELECTED_DATE";
    public static final String EXTRA_START_TIME = "START_TIME";
    public static final String EXTRA_MOVIE_TITLE = "MOVIE_TITLE";

    private List<String> selectedSeats;
    private int totalSeatPrice;
    private String showtimeId;
    private String selectedDate;
    private String startTime;
    private String movieTitle;

    private RecyclerView recyclerViewFoods;
    private TextView tvSelectedSeats;
    private TextView tvSeatPrice;
    private TextView tvFoodPrice;
    private TextView tvTotalPrice;
    private TextView tvLoginStatus;
    private MaterialCardView cardLoginStatus;
    private Button btnContinue;

    private List<Food> foods = new ArrayList<>();
    private FoodAdapter foodAdapter;
    private int totalFoodPrice = 0;

    private SessionManager sessionManager;
    private UserService userService;

    private static final String FOOD_BASE_URL = "http://10.0.2.2:8080/foods";
    private static final String RESERVATION_URL = "http://10.0.2.2:8080/reservations";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_selection);

        sessionManager = new SessionManager(this);
        userService = new UserService(this);

        if (!getIntentData()) {
            return;
        }

        setupUI();
        fetchFoods();
        handleOnBackPressed();
    }

    private boolean getIntentData() {
        selectedSeats = getIntent().getStringArrayListExtra(EXTRA_SELECTED_SEATS);
        totalSeatPrice = getIntent().getIntExtra(EXTRA_TOTAL_SEAT_PRICE, 0);
        showtimeId = getIntent().getStringExtra(EXTRA_SHOWTIME_ID);
        selectedDate = getIntent().getStringExtra(EXTRA_SELECTED_DATE);
        startTime = getIntent().getStringExtra(EXTRA_START_TIME);
//        movieTitle = getIntent().getStringExtra(EXTRA_MOVIE_TITLE);


        if (selectedSeats == null || selectedSeats.isEmpty() || showtimeId == null || selectedDate == null || startTime == null) {
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

        recyclerViewFoods = findViewById(R.id.recycler_view_foods);
        tvSelectedSeats = findViewById(R.id.tv_selected_seats);
        tvSeatPrice = findViewById(R.id.tv_seat_price);
        tvFoodPrice = findViewById(R.id.tv_food_price);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        tvLoginStatus = findViewById(R.id.tv_login_status);
        cardLoginStatus = findViewById(R.id.card_login_status);
        btnContinue = findViewById(R.id.btn_continue);

        tvSelectedSeats.setText("Ghế: " + String.join(", ", selectedSeats));
        updateLoginStatus();
        updatePrices();

        btnContinue.setOnClickListener(v -> {
            if (sessionManager.isLoggedIn()) {
                createReservationWithUserInfo();
            } else {
                showUserInfoDialog();
            }
        });

        // Khởi tạo adapter với listener mới
        foodAdapter = new FoodAdapter(foods, this);
        recyclerViewFoods.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFoods.setAdapter(foodAdapter);
    }

    // Sửa lại hàm này để lấy username đăng nhập
    private void createReservationWithUserInfo() {
        String username = sessionManager.getUserUsername(); // Lấy username thay vì name
        String phone = sessionManager.getUserPhone();

        if (username == null || phone == null) {
            Toast.makeText(this, "Thông tin người dùng không đầy đủ. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }
        createReservation(username, phone);
    }

    // Sửa lại hàm này để gửi đi một mảng đồ ăn
    private void createReservation(String username, String phone) {
        RequestQueue queue = Volley.newRequestQueue(this);
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("seats", new JSONArray(selectedSeats));

            // Lấy tất cả đồ ăn đã chọn dưới dạng mảng JSON
            JSONArray selectedFoodsArray = getSelectedFoodsAsJsonArray();
            // Đặt tên key là "foods" (số nhiều) hoặc theo yêu cầu API của bạn
            requestBody.put("foods", selectedFoodsArray);

            requestBody.put("checkin", false);
            requestBody.put("showtimeId", showtimeId);
            requestBody.put("date", selectedDate + "T00:00:00.000Z");
            requestBody.put("startAt", startTime);
            requestBody.put("username", username);
            requestBody.put("phone", phone);
            requestBody.put("total", totalSeatPrice + totalFoodPrice);

            Log.d("Reservation", "Sending request: " + requestBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, RESERVATION_URL, requestBody,
                    response -> {
                        Log.d("Reservation", "Success: " + response.toString());
                        showSuccessDialog(username, phone);
                    },
                    error -> {
                        Log.e("Reservation", "Error: " + error.toString());
                        handleReservationError(error);
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    if (sessionManager.isLoggedIn()) {
                        String token = sessionManager.getToken();
                        if (token != null && !token.isEmpty()) {
                            headers.put("Authorization", "Bearer " + token);
                        }
                    }
                    return headers;
                }
            };
            queue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo dữ liệu đặt vé", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm mới để lấy tất cả đồ ăn đã chọn
    private JSONArray getSelectedFoodsAsJsonArray() {
        JSONArray foodArray = new JSONArray();
        for (Food food : foods) {
            if (food.getQuantity() > 0) {
                try {
                    JSONObject foodObject = new JSONObject();
                    foodObject.put("foodId", food.getId());
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

    // Sửa lại hàm này cho khớp với listener mới
    @Override
    public void onQuantityChanged() {
        totalFoodPrice = 0;
        for (Food f : foods) {
            totalFoodPrice += f.getTotalPrice();
        }
        updatePrices();
    }

    // Sửa lại hàm này để hiển thị chi tiết đồ ăn
    private void showSuccessDialog(String username, String phone) {
        StringBuilder message = new StringBuilder();
        message.append("Cảm ơn ").append(username).append("!\n\n");
        message.append("📞 SĐT: ").append(phone).append("\n");
        message.append("🎬 Ghế: ").append(getSeatsString()).append("\n");

        // Tạo chuỗi chi tiết đồ ăn
        StringBuilder foodDetails = new StringBuilder();
        for (Food food : foods) {
            if (food.getQuantity() > 0) {
                foodDetails.append(" - ").append(food.getName())
                        .append(" (x").append(food.getQuantity()).append(")\n");
            }
        }

        if (foodDetails.length() > 0) {
            message.append("🍿 Đồ ăn:\n").append(foodDetails);
        }

        message.append("💰 Tổng tiền: ").append(formatPrice(totalSeatPrice + totalFoodPrice)).append("\n\n");
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


    // --- CÁC HÀM KHÁC GIỮ NGUYÊN ---

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
                btnContinue.setText("Đặt vé ngay");
            } else {
                String username = sessionManager.getUserUsername();
                tvLoginStatus.setText(username != null ? "Đang tải thông tin... (" + username + ")" : "Đang tải thông tin...");
                btnContinue.setText("Đặt vé");
                if (sessionManager.needToFetchUserInfo()) {
                    userService.fetchUserInfoIfNeeded();
                }
            }
        } else {
            tvLoginStatus.setText("Bạn đang đặt vé với tư cách khách");
            btnContinue.setText("Đặt vé");
        }
        cardLoginStatus.setVisibility(View.VISIBLE);
    }
    private void fetchFoods() {
        RequestQueue queue = Volley.newRequestQueue(this);
        Log.d("FoodSelection", "Fetching foods from: " + FOOD_BASE_URL);
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, FOOD_BASE_URL, null,
                response -> {
                    try {
                        foods.clear();
                        for (int i = 0; i < response.length(); i++) {
                            foods.add(new Food(response.getJSONObject(i)));
                        }
                        foodAdapter.notifyDataSetChanged();
                        Log.d("FoodSelection", "Loaded " + foods.size() + " foods");
                    } catch (JSONException e) {
                        Log.e("FoodSelection", "Error parsing food data", e);
                        Toast.makeText(this, "Lỗi xử lý dữ liệu đồ ăn", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("FoodSelection", "Error: " + error.toString());
                    Toast.makeText(this, "Không thể tải danh sách đồ ăn", Toast.LENGTH_SHORT).show();
                });
        queue.add(request);
    }
    private void updatePrices() {
        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        tvSeatPrice.setText(format.format(totalSeatPrice) + "đ");
        tvFoodPrice.setText(format.format(totalFoodPrice) + "đ");
        int totalPrice = totalSeatPrice + totalFoodPrice;
        tvTotalPrice.setText(format.format(totalPrice) + "đ");
    }
    private void showUserInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thông tin đặt vé");
        AlertDialog dialog = builder.create();
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_info, null);
        dialog.setView(dialogView);
        TextView tvTotal = dialogView.findViewById(R.id.tv_dialog_total);
        androidx.appcompat.widget.AppCompatEditText etUsername = dialogView.findViewById(R.id.et_username);
        androidx.appcompat.widget.AppCompatEditText etPhone = dialogView.findViewById(R.id.et_phone);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        int totalPrice = totalSeatPrice + totalFoodPrice;
        tvTotal.setText("Tổng tiền: " + formatPrice(totalPrice));
        btnConfirm.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            if (username.isEmpty() || phone.isEmpty() || !isValidPhone(phone)) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ và chính xác thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            createReservation(username, phone);
            dialog.dismiss();
        });
        dialog.setCancelable(true);
        dialog.show();
    }
    private boolean isValidPhone(String phone) {
        return phone.matches("^[0-9]{10,11}$");
    }
    private void handleReservationError(com.android.volley.VolleyError error) {
        String errorMsg = "Đặt vé thất bại. Vui lòng thử lại sau.";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            String responseBody = new String(error.networkResponse.data);
            Log.e("Reservation", "Error response: " + responseBody);
            if (responseBody.contains("occupied")) {
                errorMsg = "Ghế đã có người đặt. Vui lòng chọn lại.";
            } else {
                try {
                    JSONObject obj = new JSONObject(responseBody);
                    errorMsg = obj.optString("message", errorMsg);
                } catch (JSONException e) {
                    // Ignore
                }
            }
        }
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    }
    private String getSeatsString() {
        return String.join(", ", selectedSeats);
    }
    private String formatPrice(int price) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(price) + "đ";
    }
}
