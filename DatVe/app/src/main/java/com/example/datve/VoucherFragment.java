package com.example.datve;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class VoucherFragment extends Fragment {

    private RecyclerView recyclerViewVouchers;
    private VoucherAdapter voucherAdapter;
    private List<Voucher> voucherList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_voucher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewVouchers = view.findViewById(R.id.rv_vouchers);
        voucherList = new ArrayList<>();

        // Chuẩn bị dữ liệu voucher giả
        prepareVoucherData();

        // Thiết lập Adapter và LayoutManager
        voucherAdapter = new VoucherAdapter(getContext(), voucherList);
        recyclerViewVouchers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewVouchers.setAdapter(voucherAdapter);
    }

    private void prepareVoucherData() {
        // Dữ liệu mẫu
        voucherList.add(new Voucher("Giảm 20% combo bắp nước", "BAPNUOC20", "31/12/2025", R.drawable.voucher));
        voucherList.add(new Voucher("Miễn phí đổi vị bắp", "DOVIBAP", "30/11/2025", R.drawable.voucher1));
        voucherList.add(new Voucher("Giảm 50K vé 2D", "GIAM50K", "31/10/2025", R.drawable.voucher));
    }
}
    