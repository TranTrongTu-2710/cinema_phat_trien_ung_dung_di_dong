package com.example.datve;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class VnpayActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_payment);

        WebView webView = findViewById(R.id.webview_vnpay);
        // Bắt buộc phải bật JavaScript
        webView.getSettings().setJavaScriptEnabled(true);

        String url = getIntent().getStringExtra("paymentUrl");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Bắt sự kiện khi VNPay chuyển hướng về Return URL
                // URL này phải khớp với cấu hình trong AndroidManifest
                if (url.startsWith("datveapp://vnpay-return")) {
                    Uri uri = Uri.parse(url);
                    // Lấy mã kết quả từ VNPay
                    String responseCode = uri.getQueryParameter("vnp_ResponseCode");

                    // Tạo một Intent để gửi kết quả về cho FoodSelectionActivity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("responseCode", responseCode);
                    setResult(RESULT_OK, resultIntent);

                    // Đóng Activity thanh toán và quay về màn hình chọn đồ ăn
                    finish();
                    return true;
                }
                // Nếu không phải link trả về, cứ để WebView tải bình thường
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        // Tải URL thanh toán
        webView.loadUrl(url);
    }
}
