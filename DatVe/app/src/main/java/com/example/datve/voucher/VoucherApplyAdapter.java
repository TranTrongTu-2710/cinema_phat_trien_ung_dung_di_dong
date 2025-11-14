package com.example.datve.voucher;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datve.R;

import java.util.List;

public class VoucherApplyAdapter extends RecyclerView.Adapter<VoucherApplyAdapter.ViewHolder> {

    private final List<Voucher> voucherList;
    private final OnVoucherApplyListener listener;
    private final int currentTotal;
    private int selectedPosition = -1;

    public interface OnVoucherApplyListener {
        void onVoucherApplied(Voucher voucher);
    }

    public VoucherApplyAdapter(List<Voucher> voucherList, int currentTotal, OnVoucherApplyListener listener) {
        this.voucherList = voucherList;
        this.currentTotal = currentTotal;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voucher_apply, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Voucher voucher = voucherList.get(position);
        holder.tvVoucherName.setText(voucher.getName());
        holder.tvVoucherDesc.setText(voucher.getDescription());

        // Kiểm tra xem voucher có đủ điều kiện áp dụng không
        boolean isEligible = currentTotal >= voucher.getMinOrderTotal();

        holder.radioButton.setChecked(position == selectedPosition);
        holder.itemView.setEnabled(isEligible);
        holder.radioButton.setEnabled(isEligible);
        holder.tvVoucherName.setEnabled(isEligible);
        holder.tvVoucherDesc.setEnabled(isEligible);

        if (!isEligible) {
            holder.tvVoucherName.setTextColor(Color.GRAY);
            holder.tvVoucherDesc.setTextColor(Color.GRAY);
        } else {
            holder.tvVoucherName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
            holder.tvVoucherDesc.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray));
        }

        holder.itemView.setOnClickListener(v -> {
            if (isEligible) {
                selectedPosition = holder.getAdapterPosition();
                if (listener != null) {
                    listener.onVoucherApplied(voucher);
                }
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return voucherList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RadioButton radioButton;
        TextView tvVoucherName;
        TextView tvVoucherDesc;
        LinearLayout voucherItemRoot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.radio_button_voucher);
            tvVoucherName = itemView.findViewById(R.id.tv_item_voucher_name);
            tvVoucherDesc = itemView.findViewById(R.id.tv_item_voucher_desc);
            voucherItemRoot = itemView.findViewById(R.id.voucher_item_root);
        }
    }
}
