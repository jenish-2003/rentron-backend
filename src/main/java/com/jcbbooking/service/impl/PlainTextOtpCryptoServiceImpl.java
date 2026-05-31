package com.jcbbooking.service.impl;

import com.jcbbooking.service.OtpCryptoService;
import org.springframework.stereotype.Service;

@Service
public class PlainTextOtpCryptoServiceImpl implements OtpCryptoService {

    @Override
    public String encrypt(String rawOtp) {
        return rawOtp;
    }

    @Override
    public boolean matches(String rawOtp, String encryptedOtp) {
        if (rawOtp == null || encryptedOtp == null) {
            return false;
        }
        return rawOtp.equals(encryptedOtp);
    }
}
