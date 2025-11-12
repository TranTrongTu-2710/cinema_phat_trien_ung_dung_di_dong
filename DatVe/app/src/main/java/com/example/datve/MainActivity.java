package com.example.datve;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

// Thay đổi các import này cho đúng với cấu trúc package của bạn
import com.example.datve.movie.ViewPagerAdapter;
import com.example.datve.notification.NotificationFragment;
import com.example.datve.user.AccountFragment;
// import com.example.datve.voucher.VoucherFragment; // Tạm thời comment lại nếu chưa có
// import com.example.datve.notification.NotificationFragment; // Tạm thời comment lại nếu chưa có

import com.example.datve.user.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private View mainContentGroup;
    private View fragmentContainer;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Toolbar toolbar;

    // Sử dụng SessionManager để quảprivate SessionManager sessionManager;private SessionManager sessionManager;private SessionManager sessionManager;private SessionManager sessionManager;n lý trạng thái đăng nhập
    private SessionManager sessionManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(getApplicationContext());

        // Ánh xạ các Views từ layout
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // Đặt Toolbar làm ActionBar cho Activity

        bottomNavigation = findViewById(R.id.bottom_navigation);
        mainContentGroup = findViewById(R.id.main_content_group);
        fragmentContainer = findViewById(R.id.fragment_container);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Cấu hình cho ViewPager và TabLayout của trang chủ
        setupHomeViewPager();

        // Xử lý sự kiện khi người dùng chọn một mục trên BottomNavigationView
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                showHomeContent();
                return true;
            }
            else if (itemId == R.id.nav_voucher) {
                // Hiển thị Fragment Voucher
                replaceFragment(new VoucherFragment());

                return true;
            } else if (itemId == R.id.nav_notification) {
                // Hiển thị Fragment Thông báo
                replaceFragment(new NotificationFragment());
                return true;
            }
            else if (itemId == R.id.nav_account) {
                // Kiểm tra trạng thái đăng nhập bằng SessionManager
                if (sessionManager.isLoggedIn()) {
                    // Nếu đã đăng nhập, hiển thị Fragment Tài khoản
                    replaceFragment(new AccountFragment());
                } else {
                    // Nếu chưa đăng nhập, chuyển đến màn hình Đăng nhập
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    // Trả về false để item "Tài khoản" không được highlight trên BottomNav
                    return false;
                }
                return true;
            }
            return false;
        });

        // Mặc định hiển thị trang chủ khi ứng dụng khởi động
        showHomeContent();
    }

    /**
     * Hiển thị nội dung của trang chủ (gồm TabLayout và ViewPager)
     * và ẩn đi container chứa các fragment khác.
     */
    private void showHomeContent() {
        mainContentGroup.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Cinema App"); // Cập nhật tiêu đề Toolbar
        }
    }

    /**
     * Thay thế nội dung của fragment container bằng một Fragment mới.
     * Hàm này cũng ẩn đi nội dung của trang chủ.
     * @param fragment Fragment cần hiển thị.
     */
    private void replaceFragment(Fragment fragment) {
        mainContentGroup.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);

        // Thực hiện việc thay thế Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        // Cập nhật tiêu đề trên Toolbar cho phù hợp với Fragment hiện tại
        if (getSupportActionBar() != null) {
            if (fragment instanceof VoucherFragment) {
                getSupportActionBar().setTitle("Ví Voucher");
            } else if (fragment instanceof NotificationFragment) {
                getSupportActionBar().setTitle("Thông Báo");
            } else
            if (fragment instanceof AccountFragment) {
                getSupportActionBar().setTitle("Tài Khoản");
            }
        }
    }

    /**
     * Cấu hình ViewPagerAdapter và liên kết nó với TabLayout cho trang chủ.
     */
    private void setupHomeViewPager() {
        // Đảm bảo ViewPagerAdapter được import đúng từ package của bạn
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Phim Đang Chiếu");
            } else {
                tab.setText("Phim Sắp Chiếu");
            }
        }).attach();
    }
}
