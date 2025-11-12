package com.example.datve.movie;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datve.R;
// Sửa lại đường dẫn import cho đúng cấu trúc package
import com.example.datve.LoginActivity;
import com.example.datve.showtime.SelectShowtimeActivity;
import com.example.datve.user.SessionManager;

import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private final Context context;
    private final List<Movie> movieList;
    private final SessionManager sessionManager;

    public MovieAdapter(Context context, List<Movie> movieList) {
        this.context = context;
        this.movieList = movieList;
        this.sessionManager = new SessionManager(context);
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.movie_item, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        // Lấy đối tượng Movie tại vị trí hiện tại
        final Movie movie = movieList.get(position);

        // --- CẬP NHẬT DỮ LIỆU LÊN VIEW ---
        holder.tvTitle.setText(movie.getTitle());
        holder.tvDuration.setText(movie.getDuration() + " phút");

        // Sử dụng Glide để tải ảnh
        Glide.with(context)
                .load(movie.getImage())
                .placeholder(R.drawable.ic_launcher_background) // Ảnh tạm khi đang tải
                .error(R.drawable.ic_launcher_background)       // Ảnh tạm khi có lỗi
                .into(holder.ivPoster);

        // --- LOGIC HIỂN THỊ CÁC NHÃN (TAG) ---

        // 1. Xử lý nhãn HOT
        if (movie.getHot() == 1) {
            holder.tvHotTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvHotTag.setVisibility(View.GONE);
        }

        // 2. Xử lý nhãn giới hạn độ tuổi
        if (movie.getMinAge() > 0) {
            holder.tvAgeRating.setText("T" + movie.getMinAge());
            holder.tvAgeRating.setVisibility(View.VISIBLE);
        } else {
            holder.tvAgeRating.setVisibility(View.GONE);
        }

        // === SỰ KIỆN CLICK ĐÃ ĐƯỢC SỬA LỖI HOÀN CHỈNH ===
        holder.itemView.setOnClickListener(v -> {
            // Kiểm tra phim là "Đang chiếu" hay "Sắp chiếu"
            if (movie.getActive() != 1) {
                // Đây là phim Sắp chiếu, không cho bấm và thông báo
                Toast.makeText(context, "Phim chưa có suất chiếu.", Toast.LENGTH_SHORT).show();
            } else {
                // Đây là phim Đang chiếu, kiểm tra trạng thái đăng nhập
                if (sessionManager.isLoggedIn()) {
                    // ĐÃ ĐĂNG NHẬP: Chuyển sang màn hình chọn suất chiếu
                    Intent intent = new Intent(context, SelectShowtimeActivity.class);

                    // Bỏ ép kiểu (Parcelable) không cần thiết và gây lỗi
                    // Lớp Movie đã implements Parcelable nên có thể truyền trực tiếp
                    intent.putExtra(SelectShowtimeActivity.EXTRA_MOVIE, movie);

                    context.startActivity(intent);
                } else {
                    // CHƯA ĐĂNG NHẬP: Thông báo và chuyển sang màn hình Login
                    Toast.makeText(context, "Vui lòng đăng nhập để đặt vé", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(context, LoginActivity.class);
                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    // --- VIEW HOLDER ---
    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitle, tvDuration, tvAgeRating, tvHotTag;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_poster);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvAgeRating = itemView.findViewById(R.id.tv_age_rating);
            tvHotTag = itemView.findViewById(R.id.tv_hot_tag);
        }
    }
}
