package com.example.datve.seat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.datve.R;
import com.example.datve.food.FoodSelectionActivity;

import org.json.JSONException;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SeatSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_SHOWTIME_ID = "SHOWTIME_ID";
    public static final String EXTRA_CINEMA_ID = "CINEMA_ID";
    public static final String EXTRA_CINEMA_NAME = "CINEMA_NAME";
    public static final String EXTRA_SELECTED_DATE = "SELECTED_DATE";
    public static final String EXTRA_START_TIME = "START_TIME";

    private String showtimeId;
    private String cinemaId;
    private String cinemaName;
    private String selectedDate;
    private String startTime;

    private RecyclerView recyclerViewSeats;
    private TextView tvCinemaName;
    private TextView tvMovieInfo;
    private TextView tvSelectedSeats;
    private TextView tvTotalPrice;
    private Button btnContinue;

    private List<Seat> allSeats = new ArrayList<>();
    private List<Seat> selectedSeats = new ArrayList<>();
    private SeatRowAdapter seatRowAdapter;

    private static final String CINEMA_BASE_URL = "http://10.0.2.2:8080/cinemas/";
    private static final String OCCUPIED_BASE_URL = "http://10.0.2.2:8080/reservations/occupied";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        // Lấy dữ liệu từ Intent
        showtimeId = getIntent().getStringExtra(EXTRA_SHOWTIME_ID);
        cinemaId = getIntent().getStringExtra(EXTRA_CINEMA_ID);
        cinemaName = getIntent().getStringExtra(EXTRA_CINEMA_NAME);
        selectedDate = getIntent().getStringExtra(EXTRA_SELECTED_DATE);
        startTime = getIntent().getStringExtra(EXTRA_START_TIME);

        if (showtimeId == null || cinemaId == null) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin suất chiếu", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        fetchCinemaSeats();
    }

    private void setupUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerViewSeats = findViewById(R.id.recycler_view_seats);
        tvCinemaName = findViewById(R.id.tv_cinema_name);
        tvMovieInfo = findViewById(R.id.tv_movie_info);
        tvSelectedSeats = findViewById(R.id.tv_selected_seats);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        btnContinue = findViewById(R.id.btn_continue);

        // Hiển thị thông tin
        tvCinemaName.setText(cinemaName != null ? cinemaName : "");
        tvMovieInfo.setText(String.format("Ngày %s • %s",
                selectedDate != null ? selectedDate : "",
                startTime != null ? startTime : ""));

        btnContinue.setOnClickListener(v -> {
            if (selectedSeats.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế", Toast.LENGTH_SHORT).show();
                return;
            }

            // Chuyển sang màn hình chọn đồ ăn
            Intent intent = new Intent(this, FoodSelectionActivity.class);

            // Truyền danh sách ghế đã chọn
            ArrayList<String> seatNumbers = new ArrayList<>();
            for (Seat seat : selectedSeats) {
                seatNumbers.add(seat.getSeatNumber());
            }
            intent.putStringArrayListExtra(FoodSelectionActivity.EXTRA_SELECTED_SEATS, seatNumbers);

            // Truyền tổng tiền ghế
            int totalSeatPrice = 0;
            for (Seat seat : selectedSeats) {
                totalSeatPrice += seat.getPrice();
            }
            intent.putExtra(FoodSelectionActivity.EXTRA_TOTAL_SEAT_PRICE, totalSeatPrice);

            // Truyền thông tin suất chiếu
            intent.putExtra(FoodSelectionActivity.EXTRA_SHOWTIME_ID, showtimeId);
            intent.putExtra(FoodSelectionActivity.EXTRA_SELECTED_DATE, selectedDate);
            intent.putExtra(FoodSelectionActivity.EXTRA_START_TIME, startTime);

            startActivity(intent);
        });

        recyclerViewSeats.setLayoutManager(new LinearLayoutManager(this));
    }

    private void fetchCinemaSeats() {
        String url = CINEMA_BASE_URL + cinemaId;
        RequestQueue queue = Volley.newRequestQueue(this);

        Log.d("SeatSelection", "Fetching cinema seats from: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        CinemaResponse cinemaResponse = new CinemaResponse(response);
                        createSeatList(cinemaResponse);
                        fetchOccupiedSeats();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Lỗi xử lý dữ liệu rạp", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("SeatSelection", "Error: " + error.toString());
                    Toast.makeText(this, "Không thể tải thông tin rạp", Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }

    private void fetchOccupiedSeats() {
        // Build URL với query parameters
        String url = OCCUPIED_BASE_URL + "?showtimeId=" + showtimeId + "&date=" + selectedDate + "&startAt=" + startTime;
        RequestQueue queue = Volley.newRequestQueue(this);

        Log.d("SeatSelection", "Fetching occupied seats from: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        OccupiedSeatsResponse occupiedResponse = new OccupiedSeatsResponse(response);
                        markOccupiedSeats(occupiedResponse.getOccupied());
                        displaySeats();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Lỗi xử lý dữ liệu ghế đã bán", Toast.LENGTH_SHORT).show();
                        displaySeats(); // Vẫn hiển thị ghế nếu có lỗi
                    }
                },
                error -> {
                    Log.e("SeatSelection", "Error: " + error.toString());
                    Toast.makeText(this, "Không thể tải thông tin ghế đã bán", Toast.LENGTH_SHORT).show();
                    displaySeats(); // Vẫn hiển thị ghế nếu có lỗi
                });

        queue.add(request);
    }

    private void createSeatList(CinemaResponse cinemaResponse) {
        allSeats.clear();

        for (CinemaResponse.SeatGroup seatGroup : cinemaResponse.getSeats()) {
            Seat.SeatType type = seatGroup.getType().equals("VIP")
                    ? Seat.SeatType.VIP
                    : Seat.SeatType.REG;

            for (String seatNumber : seatGroup.getListSeats()) {
                Seat seat = new Seat(seatNumber, type, seatGroup.getPrice());
                allSeats.add(seat);
            }
        }

        Log.d("SeatSelection", "Created " + allSeats.size() + " seats");
    }

    private void markOccupiedSeats(List<String> occupiedSeatNumbers) {
        for (Seat seat : allSeats) {
            if (occupiedSeatNumbers.contains(seat.getSeatNumber())) {
                seat.setStatus(Seat.SeatStatus.OCCUPIED);
            }
        }
        Log.d("SeatSelection", "Marked " + occupiedSeatNumbers.size() + " seats as occupied");
    }

    private void displaySeats() {
        // Nhóm ghế theo hàng (A, B, C...)
        Map<Character, List<Seat>> seatsByRow = new HashMap<>();
        for (Seat seat : allSeats) {
            char row = seat.getRow();
            if (!seatsByRow.containsKey(row)) {
                seatsByRow.put(row, new ArrayList<>());
            }
            seatsByRow.get(row).add(seat);
        }

        // Sắp xếp các hàng theo thứ tự alphabet
        List<Character> sortedRows = new ArrayList<>(seatsByRow.keySet());
        sortedRows.sort(Character::compareTo);

        // Sắp xếp ghế trong mỗi hàng theo số
        for (List<Seat> seats : seatsByRow.values()) {
            seats.sort((s1, s2) -> {
                try {
                    int num1 = Integer.parseInt(s1.getColumn());
                    int num2 = Integer.parseInt(s2.getColumn());
                    return Integer.compare(num1, num2);
                } catch (NumberFormatException e) {
                    return s1.getColumn().compareTo(s2.getColumn());
                }
            });
        }

        // Tạo Map đã sắp xếp
        Map<Character, List<Seat>> sortedSeatsByRow = new HashMap<>();
        for (Character row : sortedRows) {
            sortedSeatsByRow.put(row, seatsByRow.get(row));
        }

        seatRowAdapter = new SeatRowAdapter(sortedSeatsByRow, this::onSeatClick);
        recyclerViewSeats.setAdapter(seatRowAdapter);

        updateSummary();
    }

    private void onSeatClick(Seat seat) {
        if (seat.getStatus() == Seat.SeatStatus.OCCUPIED) {
            Toast.makeText(this, "Ghế " + seat.getSeatNumber() + " đã có người đặt", Toast.LENGTH_SHORT).show();
            return;
        }

        // Toggle trạng thái ghế
        if (seat.getStatus() == Seat.SeatStatus.SELECTED) {
            seat.setStatus(Seat.SeatStatus.AVAILABLE);
            selectedSeats.remove(seat);
        } else {
            seat.setStatus(Seat.SeatStatus.SELECTED);
            selectedSeats.add(seat);
        }

        // Cập nhật UI
        if (seatRowAdapter != null) {
            seatRowAdapter.notifyDataSetChanged();
        }
        updateSummary();
    }

    private void updateSummary() {
        if (selectedSeats.isEmpty()) {
            tvSelectedSeats.setText("Ghế đã chọn: Chưa chọn");
            tvTotalPrice.setText("0đ");
            btnContinue.setEnabled(false);
        } else {
            // Hiển thị danh sách ghế đã chọn
            StringBuilder seatList = new StringBuilder("Ghế đã chọn: ");
            for (int i = 0; i < selectedSeats.size(); i++) {
                seatList.append(selectedSeats.get(i).getSeatNumber());
                if (i < selectedSeats.size() - 1) {
                    seatList.append(", ");
                }
            }
            tvSelectedSeats.setText(seatList.toString());

            // Tính tổng tiền
            int totalPrice = 0;
            for (Seat seat : selectedSeats) {
                totalPrice += seat.getPrice();
            }

            NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
            tvTotalPrice.setText(format.format(totalPrice) + "đ");
            btnContinue.setEnabled(true);
        }
    }

    private void proceedToPayment() {
        // TODO: Implement payment flow
        // Chuyển sang màn hình thanh toán với danh sách ghế đã chọn
        Toast.makeText(this, "Đang chuyển đến thanh toán...", Toast.LENGTH_SHORT).show();

        // Ví dụ:
        // Intent intent = new Intent(this, PaymentActivity.class);
        // intent.putExtra("selected_seats", new ArrayList<>(selectedSeats));
        // intent.putExtra("total_price", calculateTotalPrice());
        // startActivity(intent);
    }
}