package com.example.datve;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    private Context context;
    private List<Voucher> voucherList;

    public VoucherAdapter(Context context, List<Voucher> voucherList) {
        this.context = context;
        this.voucherList = voucherList;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.voucher_item, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        Voucher voucher = voucherList.get(position);
        holder.tvTitle.setText(voucher.getTitle());
        holder.tvCode.setText("Mã: " + voucher.getCode());
        holder.tvExpiry.setText("HSD: " + voucher.getExpiryDate());
        holder.ivImage.setImageResource(voucher.getImageResId());

        holder.btnUse.setOnClickListener(v -> {
            Toast.makeText(context, "Sử dụng voucher: " + voucher.getCode(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return voucherList.size();
    }

    public static class VoucherViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle, tvCode, tvExpiry;
        Button btnUse;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_voucher_image);
            tvTitle = itemView.findViewById(R.id.tv_voucher_title);
            tvCode = itemView.findViewById(R.id.tv_voucher_code);
            tvExpiry = itemView.findViewById(R.id.tv_voucher_expiry);
            btnUse = itemView.findViewById(R.id.btn_use_voucher);
        }
    }
}
    