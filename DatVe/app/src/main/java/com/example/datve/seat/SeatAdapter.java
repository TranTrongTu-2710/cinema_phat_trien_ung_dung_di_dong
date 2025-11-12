package com.example.datve.seat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datve.R;

import java.util.List;

public class SeatAdapter extends RecyclerView.Adapter<SeatAdapter.SeatViewHolder> {

    private final List<Seat> seats;
    // Sử dụng OnSeatClickListener công khai
    private final OnSeatClickListener listener;

    // Interface OnSeatClickListener đã được xóa khỏi đây

    // Constructor bây giờ mong đợi một đối tượng OnSeatClickListener công khai
    public SeatAdapter(List<Seat> seats, OnSeatClickListener listener) {
        this.seats = seats;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SeatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.seat_item, parent, false);
        return new SeatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeatViewHolder holder, int position) {
        Seat seat = seats.get(position);
        holder.bind(seat, listener);
    }

    @Override
    public int getItemCount() {
        return seats.size();
    }

    static class SeatViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivSeat;
        private final TextView tvSeatLabel;

        public SeatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSeat = itemView.findViewById(R.id.iv_seat);
            tvSeatLabel = itemView.findViewById(R.id.tv_seat_label);
        }

        public void bind(final Seat seat, final OnSeatClickListener listener) {
            // Hiển thị số ghế (chỉ lấy phần số)
            tvSeatLabel.setText(seat.getColumn());

            // Cập nhật drawable dựa trên trạng thái
            int drawableRes;
            switch (seat.getStatus()) {
                case SELECTED:
                    drawableRes = R.drawable.seat_selected;
                    break;
                case OCCUPIED:
                    drawableRes = R.drawable.seat_occupied;
                    break;
                case AVAILABLE:
                default:
                    // Nếu là VIP thì dùng màu khác
                    drawableRes = seat.getType() == Seat.SeatType.VIP
                            ? R.drawable.seat_vip
                            : R.drawable.seat_available;
                    break;
            }
            ivSeat.setImageResource(drawableRes);

            // Xử lý click
            if (seat.getStatus() == Seat.SeatStatus.OCCUPIED) {
                // Ghế đã bán - không cho click
                itemView.setEnabled(false);
                itemView.setOnClickListener(null);
            } else {
                itemView.setEnabled(true);
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSeatClick(seat);
                    }
                });
            }
        }
    }
}
