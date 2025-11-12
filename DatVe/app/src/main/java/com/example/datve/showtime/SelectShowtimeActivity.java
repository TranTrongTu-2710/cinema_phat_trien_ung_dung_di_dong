package com.example.datve.showtime;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
import com.bumptech.glide.Glide;
import com.example.datve.R;
import com.example.datve.movie.Movie;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SelectShowtimeActivity extends AppCompatActivity {

    public static final String EXTRA_MOVIE = "extra_movie";

    private Movie movie;
    private List<Showtime> allShowtimes = new ArrayList<>();
    private ShowtimeAdapter showtimeAdapter;
    private TextView tvNoShowtimes;
    private Date selectedDate; // Ngày được chọn từ các nút ngày
    private static final String SHOWTIMES_BASE_URL = "http://10.0.2.2:8080/showtimes/by_movie/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_showtime);

        movie = getIntent().getParcelableExtra(EXTRA_MOVIE);
        if (movie == null) {
            Toast.makeText(this, "Lỗi: Không nhận được dữ liệu phim.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();

        // Tạo 7 ngày tiếp theo và thiết lập RecyclerView ngày
        List<Date> next7Days = generateNext7Days();
        setupDayRecyclerView(next7Days);

        // Mặc định chọn ngày đầu tiên (hôm nay)
        selectedDate = next7Days.get(0);

        // Thiết lập RecyclerView suất chiếu
        setupShowtimeRecyclerView();

        // Fetch TẤT CẢ suất chiếu từ server (không lọc theo ngày)
        fetchShowtimes();
    }

    private void setupUI() {
        Toolbar toolbar = findViewById(R.id.toolbar_select_showtime);
        if (toolbar == null) {
            Log.e("SelectShowtimeActivity", "Toolbar is null! Check your layout file.");
            return;
        }
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(movie.getTitle());

        ImageView ivBackdrop = findViewById(R.id.iv_movie_backdrop);
        TextView tvTitle = findViewById(R.id.tv_movie_title);
        TextView tvGenreDuration = findViewById(R.id.tv_movie_genre_duration);
        TextView tvDescription = findViewById(R.id.tv_movie_description);
        tvNoShowtimes = findViewById(R.id.tv_no_showtimes);

        tvTitle.setText(movie.getTitle());
        tvGenreDuration.setText(String.format("%s | %d phút", movie.getGenre(), movie.getDuration()));
        tvDescription.setText(movie.getDescription());

        Glide.with(this).load(movie.getImage()).into(ivBackdrop);
    }

    private void setupShowtimeRecyclerView() {
        RecyclerView rvShowtimes = findViewById(R.id.recycler_view_showtimes);
        rvShowtimes.setLayoutManager(new LinearLayoutManager(this));
        // Truyền selectedDate vào adapter để sử dụng khi nhấn nút "Chọn"
        showtimeAdapter = new ShowtimeAdapter(this, new ArrayList<>(), () -> selectedDate);
        rvShowtimes.setAdapter(showtimeAdapter);
    }

    private void fetchShowtimes() {
        String url = SHOWTIMES_BASE_URL + movie.getId();
        RequestQueue queue = Volley.newRequestQueue(this);

        Log.d("SelectShowtime", "Fetching showtimes from: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d("SelectShowtime", "Response received: " + response.toString());
                    allShowtimes.clear();
                    try {
                        JSONArray itemsArray = response.getJSONArray("items");

                        for (int i = 0; i < itemsArray.length(); i++) {
                            JSONObject showtimeObject = itemsArray.getJSONObject(i);
                            allShowtimes.add(new Showtime(showtimeObject));
                        }

                        Log.d("SelectShowtime", "Loaded " + allShowtimes.size() + " showtimes");

                        // Hiển thị TẤT CẢ suất chiếu (không lọc)
                        if (allShowtimes.isEmpty()) {
                            tvNoShowtimes.setText("Phim hiện chưa có suất chiếu.");
                            tvNoShowtimes.setVisibility(View.VISIBLE);
                        } else {
                            tvNoShowtimes.setVisibility(View.GONE);
                            showtimeAdapter.updateData(allShowtimes);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Lỗi xử lý dữ liệu suất chiếu", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("SelectShowtime", "Volley Error: " + error.toString());
                    error.printStackTrace();
                    Toast.makeText(this, "Không thể tải suất chiếu", Toast.LENGTH_SHORT).show();
                    tvNoShowtimes.setText("Không thể tải suất chiếu. Vui lòng thử lại.");
                    tvNoShowtimes.setVisibility(View.VISIBLE);
                });

        request.setShouldCache(false);
        queue.add(request);
    }

    /**
     * Tạo danh sách 7 ngày tiếp theo (bắt đầu từ hôm nay)
     */
    private List<Date> generateNext7Days() {
        List<Date> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // Reset giờ, phút, giây về 0
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 7; i++) {
            dates.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Log.d("SelectShowtime", "Generated " + dates.size() + " days");
        return dates;
    }

    /**
     * Thiết lập RecyclerView cho danh sách ngày
     */
    private void setupDayRecyclerView(List<Date> days) {
        RecyclerView rvDays = findViewById(R.id.recycler_view_days);
        rvDays.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        DayAdapter dayAdapter = new DayAdapter(days, this::onDaySelected);
        rvDays.setAdapter(dayAdapter);

        Log.d("SelectShowtime", "Day RecyclerView setup with " + days.size() + " items");
    }

    /**
     * Callback khi người dùng chọn ngày
     * CHỈ CẬP NHẬT selectedDate, KHÔNG lọc showtime
     */
    private void onDaySelected(Date date) {
        selectedDate = date;
        Log.d("SelectShowtime", "Selected date: " + date);
        // Không làm gì thêm - chỉ lưu ngày đã chọn
    }
}