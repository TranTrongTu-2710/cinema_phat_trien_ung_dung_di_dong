package com.example.datve.voucher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datve.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    private final Context context;
    private final List<Voucher> voucherList;

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

        // Cập nhật để hiển thị dữ liệu mới
        holder.tvVoucherName.setText(voucher.getName());
        holder.tvVoucherDesc.setText(voucher.getDescription());
        holder.tvExpiryDate.setText("HSD: " + voucher.getEndAt());
//        holder.ivVoucher.setImageResource(voucher.getImageResId());

        holder.btnCopyCode.setOnClickListener(v -> {
            // Logic sao chép mã (bạn có thể tự triển khai)
            Toast.makeText(context, "Đã sao chép mã: " + voucher.getCode(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return voucherList.size();
    }

    public static class VoucherViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVoucher;
        TextView tvVoucherName, tvVoucherDesc, tvExpiryDate;
        MaterialButton btnCopyCode;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVoucher = itemView.findViewById(R.id.iv_voucher);
            tvVoucherName = itemView.findViewById(R.id.tv_voucher_name);
            tvVoucherDesc = itemView.findViewById(R.id.tv_voucher_desc);
            tvExpiryDate = itemView.findViewById(R.id.tv_expiry_date);
            btnCopyCode = itemView.findViewById(R.id.btn_copy_code);
        }
    }
}
