package com.example.datve;

import android.app.Application;
import android.util.Log;

import com.example.datve.user.SessionManager;
import com.example.datve.user.UserService;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static MyApplication instance;
    private SessionManager sessionManager;
    private UserService userService;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate");
        instance = this;

        // Khởi tạo SessionManager và UserService
        sessionManager = new SessionManager(this);
        userService = new UserService(this);

        // Tự động lấy thông tin user khi app khởi động (nếu đã đăng nhập)
        fetchUserInfoInBackground();
    }

    public static MyApplication getInstance() {
        return instance;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public UserService getUserService() {
        return userService;
    }

    /**
     * Lấy thông tin user trong background (không ảnh hưởng UI)
     */
    private void fetchUserInfoInBackground() {
        new Thread(() -> {
            try {
                // Đợi một chút để app khởi động xong
                Thread.sleep(2000); // Tăng thời gian chờ lên 2 giây

                if (sessionManager.needToFetchUserInfo()) {
                    Log.d(TAG, "Auto-fetching user info on app start");
                    userService.fetchUserInfoIfNeeded();
                } else {
                    Log.d(TAG, "No need to fetch user info on app start");
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Background thread interrupted", e);
            }
        }).start();
    }
}