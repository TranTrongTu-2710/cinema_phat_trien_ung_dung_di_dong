package com.example.datve.showtime;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.datve.R;
import com.example.datve.seat.SeatSelectionActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShowtimeAdapter extends RecyclerView.Adapter<ShowtimeAdapter.ShowtimeViewHolder> {

    private final Context context;
    private final List<Showtime> showtimeList;
    private final SelectedDateProvider selectedDateProvider;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Interface để lấy ngày đã chọn từ Activity
    public interface SelectedDateProvider {
        Date getSelectedDate();
    }

    public ShowtimeAdapter(Context context, List<Showtime> showtimeList, SelectedDateProvider selectedDateProvider) {
        this.context = context;
        this.showtimeList = showtimeList;
        this.selectedDateProvider = selectedDateProvider;
    }

    @NonNull
    @Override
    public ShowtimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.showtime_item, parent, false);
        return new ShowtimeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShowtimeViewHolder holder, int position) {
        Showtime showtime = showtimeList.get(position);
        holder.tvCinemaName.setText(showtime.getCinemaName());
        holder.tvTime.setText("Bắt đầu lúc " + showtime.getStartAt());

        holder.btnSelect.setOnClickListener(v -> {
            // Lấy ngày đã chọn từ Activity
            Date selectedDate = selectedDateProvider.getSelectedDate();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dateString = dateFormat.format(selectedDate);

            // Chuyển sang màn hình chọn ghế
            Intent intent = new Intent(context, SeatSelectionActivity.class);
            intent.putExtra(SeatSelectionActivity.EXTRA_SHOWTIME_ID, showtime.getId());
            intent.putExtra(SeatSelectionActivity.EXTRA_CINEMA_ID, showtime.getCinemaId());
            intent.putExtra(SeatSelectionActivity.EXTRA_CINEMA_NAME, showtime.getCinemaName());
            intent.putExtra(SeatSelectionActivity.EXTRA_SELECTED_DATE, dateString);
            intent.putExtra(SeatSelectionActivity.EXTRA_START_TIME, showtime.getStartAt());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return showtimeList.size();
    }

    public void updateData(List<Showtime> newShowtimes) {
        showtimeList.clear();
        showtimeList.addAll(newShowtimes);
        notifyDataSetChanged();
    }

    static class ShowtimeViewHolder extends RecyclerView.ViewHolder {
        TextView tvCinemaName, tvTime;
        Button btnSelect;

        public ShowtimeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCinemaName = itemView.findViewById(R.id.tv_cinema_name);
            tvTime = itemView.findViewById(R.id.tv_showtime_time);
            btnSelect = itemView.findViewById(R.id.btn_select_showtime);
        }
    }
}