package com.example.datve.showtime;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datve.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    private final List<Date> dayList;
    private int selectedPosition = 0;
    private final OnDayClickListener listener;

    // "dd/MM", ví dụ 10/11
    private final SimpleDateFormat dayMonthFormat =
            new SimpleDateFormat("dd/MM", Locale.getDefault());

    public interface OnDayClickListener {
        void onDayClick(Date date);
    }

    public DayAdapter(List<Date> dayList, OnDayClickListener listener) {
        this.dayList = dayList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.day_item, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, final int position) {
        Date date = dayList.get(position);
        holder.tvDayOfMonth.setText(dayMonthFormat.format(date));

        boolean isSelected = (selectedPosition == position);

        // ---- QUAN TRỌNG: set selected cho view có selector ----
        holder.container.setSelected(isSelected);
        holder.tvDayOfMonth.setSelected(isSelected);

        // ---- Fallback nếu bạn CHƯA dùng selector màu chữ/nền trong XML ----
        if (isSelected) {
            holder.tvDayOfMonth.setTextColor(Color.WHITE);
            applyRoundedBg(holder.container, Color.parseColor("#D32F2F"), // đỏ
                    12f, /*radius*/ true); // không vẽ viền khi chọn
        } else {
            holder.tvDayOfMonth.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.black));
            // nền trắng + viền xám nhạt
            applyRoundedBg(holder.container, Color.WHITE, 12f, false);
        }

        holder.itemView.setOnClickListener(v -> {
            if (position != selectedPosition) {
                int previous = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(previous);
                notifyItemChanged(selectedPosition);
                if (listener != null) listener.onDayClick(dayList.get(selectedPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return dayList.size();
    }

    // Cho phép set selected từ ngoài nếu cần
    public void setSelectedPosition(int pos) {
        int old = this.selectedPosition;
        this.selectedPosition = pos;
        notifyItemChanged(old);
        notifyItemChanged(this.selectedPosition);
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final View container;       // layout cha để đổi nền
        final TextView tvDayOfMonth;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            // Nếu trong day_item.xml có id cho container, dùng nó; nếu không thì dùng itemView
            View c = itemView.findViewById(R.id.day_item_container);
            container = (c != null) ? c : itemView;
            tvDayOfMonth = itemView.findViewById(R.id.tv_day_of_month);
        }
    }

    // ===== Helper: tạo nền tròn bo góc, có/không có viền =====
    private static void applyRoundedBg(View v, int solidColor, float radiusDp, boolean noStroke) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(solidColor);
        float r = v.getResources().getDisplayMetrics().density * radiusDp;
        d.setCornerRadius(r);
        if (!noStroke) {
            d.setStroke((int) (v.getResources().getDisplayMetrics().density * 1),
                    Color.parseColor("#DDDDDD"));
        }
        v.setBackground(d);
    }
}
