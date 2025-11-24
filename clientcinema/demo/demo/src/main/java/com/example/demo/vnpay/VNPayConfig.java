package com.example.demo.vnpay;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VNPayConfig {

    @Value("${vnpay.url}")
    private String vnpUrl;

    @Value("${vnpay.returnUrl}")
    private String vnpReturnUrl;

    @Value("${vnpay.notifyUrl}")
    private String vnpNotifyUrl;

    @Value("${vnpay.tmnCode}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnpHashSecret;

    @Value("${vnpay.version}")
    private String vnpVersion;

    @Value("${vnpay.command}")
    private String vnpCommand;

    @Value("${vnpay.orderType}")
    private String orderType;
}