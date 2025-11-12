package com.example.datve.movie; // Thay đổi package nếu cần

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;


public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // ĐÃ THAY ĐỔI: Đảo vị trí của hai Fragment
        if (position == 0) {
            // Vị trí 0 (mặc định) bây giờ là Phim Đang Chiếu
            return new NowShowingFragment();
        } else {
            // Vị trí 1 là Phim Sắp Chiếu
            return new ComingSoonFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Chúng ta có 2 tab
    }
}
