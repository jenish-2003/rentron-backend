package com.jcbbooking.service;

public interface OtpCryptoService {
    String encrypt(String rawOtp);
    boolean matches(String rawOtp, String encryptedOtp);
}
