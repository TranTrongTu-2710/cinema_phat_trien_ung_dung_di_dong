package com.example.datve.payment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.datve.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class VNPayPaymentActivity extends AppCompatActivity {

    private static final String TAG = "VNPayPayment";
    private static final String PAYMENT_URL = "http://10.0.2.2:8080/api/payment/create-vnpay-payment";

    private WebView webView;
    private ProgressDialog progressDialog;
    private String reservationId;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_payment);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thanh toán VNPay");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        webView = findViewById(R.id.webview_vnpay);
        reservationId = getIntent().getStringExtra("reservationId");

        if (reservationId == null || reservationId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin đặt vé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tạo liên kết thanh toán...");
        progressDialog.setCancelable(false);

        // Setup WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setWebViewClient(new VNPayWebViewClient());

        // Tạo payment URL
        createPaymentUrl(reservationId);
    }

    /**
     * Gọi API để tạo VNPay payment URL
     */
    private void createPaymentUrl(String reservationId) {
        progressDialog.show();

        String url = PAYMENT_URL + "?reservationId=" + reservationId;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    progressDialog.dismiss();
                    try {
                        String code = response.optString("code", "");
                        String paymentUrl = response.optString("paymentUrl", "");

                        if ("00".equals(code) && !paymentUrl.isEmpty()) {
                            Log.d(TAG, "Payment URL: " + paymentUrl);
                            webView.loadUrl(paymentUrl);
                        } else {
                            String message = response.optString("message", "Không thể tạo liên kết thanh toán");
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response", e);
                        Toast.makeText(this, "Lỗi xử lý dữ liệu", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error creating payment", error);

                    String errorMsg = "Lỗi kết nối";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data);
                            JSONObject obj = new JSONObject(responseBody);
                            errorMsg = obj.optString("error", errorMsg);
                        } catch (JSONException e) {
                            // Ignore
                        }
                    }

                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                    finish();
                }
        );

        queue.add(request);
    }

    /**
     * WebViewClient để bắt URL callback từ VNPay
     */
    private class VNPayWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "Loading URL: " + url);

            // Kiểm tra nếu là return URL từ VNPay
            if (url.contains("vnp_ResponseCode") || url.contains("/payment/vnpay-return")) {
                progressDialog.setMessage("Đang xác nhận thanh toán...");
                progressDialog.show();

                // Parse URL parameters
                Uri uri = Uri.parse(url);
                Map<String, String> params = new HashMap<>();

                for (String paramName : uri.getQueryParameterNames()) {
                    String paramValue = uri.getQueryParameter(paramName);
                    if (paramValue != null) {
                        params.put(paramName, paramValue);
                    }
                }

                // Xử lý kết quả thanh toán
                handlePaymentResult(params);
                return true;
            }

            // Load URL bình thường
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (!progressDialog.isShowing()) {
                progressDialog.setMessage("Đang tải...");
                progressDialog.show();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Log.e(TAG, "WebView error: " + description);
            progressDialog.dismiss();
            Toast.makeText(VNPayPaymentActivity.this, "Lỗi tải trang: " + description, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Xử lý kết quả thanh toán
     */
    private void handlePaymentResult(Map<String, String> params) {
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");

        Log.d(TAG, "Payment result - ResponseCode: " + responseCode + ", TransactionNo: " + transactionNo);

        if ("00".equals(responseCode)) {
            // Thanh toán thành công
            progressDialog.dismiss();
            Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_LONG).show();

            // Trả kết quả về FoodSelectionActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("success", true);
            resultIntent.putExtra("reservationId", reservationId);
            resultIntent.putExtra("transactionNo", transactionNo);
            setResult(RESULT_OK, resultIntent);
            finish();

        } else {
            // Thanh toán thất bại
            progressDialog.dismiss();
            String errorMessage = getPaymentErrorMessage(responseCode);
            Toast.makeText(this, "Thanh toán thất bại: " + errorMessage, Toast.LENGTH_LONG).show();

            // Trả kết quả về FoodSelectionActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("success", false);
            resultIntent.putExtra("errorCode", responseCode);
            resultIntent.putExtra("errorMessage", errorMessage);
            setResult(RESULT_CANCELED, resultIntent);
            finish();
        }
    }

    /**
     * Lấy message lỗi từ response code
     */
    private String getPaymentErrorMessage(String responseCode) {
        if (responseCode == null) return "Lỗi không xác định";

        switch (responseCode) {
            case "07":
                return "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09":
                return "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking.";
            case "10":
                return "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.";
            case "11":
                return "Đã hết hạn chờ thanh toán. Vui lòng thực hiện lại.";
            case "12":
                return "Thẻ/Tài khoản bị khóa.";
            case "13":
                return "Nhập sai mật khẩu xác thực giao dịch (OTP).";
            case "24":
                return "Khách hàng hủy giao dịch.";
            case "51":
                return "Tài khoản không đủ số dư.";
            case "65":
                return "Tài khoản đã vượt quá hạn mức giao dịch trong ngày.";
            case "75":
                return "Ngân hàng thanh toán đang bảo trì.";
            case "79":
                return "Nhập sai mật khẩu thanh toán quá số lần quy định.";
            default:
                return "Lỗi không xác định (Mã: " + responseCode + ")";
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // Xác nhận hủy thanh toán
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có muốn hủy thanh toán?")
                    .setPositiveButton("Có", (dialog, which) -> {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("success", false);
                        resultIntent.putExtra("cancelled", true);
                        setResult(RESULT_CANCELED, resultIntent);
                        super.onBackPressed();
                    })
                    .setNegativeButton("Không", null)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (webView != null) {
            webView.destroy();
        }
    }
}