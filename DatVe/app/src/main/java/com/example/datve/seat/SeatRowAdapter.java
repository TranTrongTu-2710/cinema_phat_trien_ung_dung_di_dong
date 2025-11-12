package com.example.datve.seat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datve.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SeatRowAdapter extends RecyclerView.Adapter<SeatRowAdapter.SeatRowViewHolder> {

    private final Map<Character, List<Seat>> seatsByRow;
    private final List<Character> sortedRows;
    // Sử dụng OnSeatClickListener công khai
    private final OnSeatClickListener listener;

    // Interface OnSeatClickListener đã được xóa khỏi đây

    public SeatRowAdapter(Map<Character, List<Seat>> seatsByRow, OnSeatClickListener listener) {
        this.seatsByRow = seatsByRow;
        this.listener = listener;
        // Sắp xếp các hàng theo thứ tự A, B, C...
        this.sortedRows = new ArrayList<>(seatsByRow.keySet());
        Collections.sort(this.sortedRows);
    }

    @NonNull
    @Override
    public SeatRowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.seat_row_item, parent, false);
        return new SeatRowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeatRowViewHolder holder, int position) {
        Character rowChar = sortedRows.get(position);
        List<Seat> seatsInRow = seatsByRow.get(rowChar);
        // Sắp xếp các ghế trong hàng theo thứ tự 1, 2, 3...
        if (seatsInRow != null) {
            Collections.sort(seatsInRow, Comparator.comparingInt(seat -> Integer.parseInt(seat.getColumn())));
            holder.bind(rowChar, seatsInRow, listener);
        }
    }

    @Override
    public int getItemCount() {
        return sortedRows.size();
    }

    static class SeatRowViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRowLabel;
        private final RecyclerView recyclerViewSeats;

        public SeatRowViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRowLabel = itemView.findViewById(R.id.tv_row_label);
            recyclerViewSeats = itemView.findViewById(R.id.recycler_view_seats_in_row);
        }

        public void bind(Character rowChar, List<Seat> seatsInRow, OnSeatClickListener listener) {
            tvRowLabel.setText(String.valueOf(rowChar));

            // Dòng này bây giờ đã hợp lệ vì cả hai đều dùng chung một kiểu listener
            SeatAdapter seatAdapter = new SeatAdapter(seatsInRow, listener);
            recyclerViewSeats.setAdapter(seatAdapter);

            // Thiết lập LayoutManager cho RecyclerView con (hiển thị ghế theo chiều ngang)
            recyclerViewSeats.setLayoutManager(new LinearLayoutManager(
                    itemView.getContext(),
                    RecyclerView.HORIZONTAL,
                    false
            ));
        }
    }
}
