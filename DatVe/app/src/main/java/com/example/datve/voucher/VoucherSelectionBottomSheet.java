package com.example.datve.voucher;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datve.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VoucherSelectionBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_VOUCHERS = "vouchers";
    private static final String ARG_CURRENT_TOTAL = "current_total";

    private List<Voucher> voucherList;
    private int currentTotal;
    private VoucherSelectionListener mListener;

    public interface VoucherSelectionListener {
        void onVoucherSelected(Voucher voucher);
    }

    public static VoucherSelectionBottomSheet newInstance(ArrayList<Voucher> vouchers, int currentTotal) {
        VoucherSelectionBottomSheet fragment = new VoucherSelectionBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_VOUCHERS, vouchers);
        args.putInt(ARG_CURRENT_TOTAL, currentTotal);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof VoucherSelectionListener) {
            mListener = (VoucherSelectionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement VoucherSelectionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            voucherList = (List<Voucher>) getArguments().getSerializable(ARG_VOUCHERS);
            currentTotal = getArguments().getInt(ARG_CURRENT_TOTAL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_voucher_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.rv_bottom_sheet_vouchers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        VoucherApplyAdapter adapter = new VoucherApplyAdapter(voucherList, currentTotal, voucher -> {
            if (mListener != null) {
                mListener.onVoucherSelected(voucher);
            }
            dismiss(); // Tự động đóng BottomSheet sau khi chọn
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
