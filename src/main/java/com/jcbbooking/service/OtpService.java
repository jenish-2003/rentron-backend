package com.jcbbooking.service;

import com.jcbbooking.model.OtpPurpose;
import com.jcbbooking.model.OtpVerification;

public interface OtpService {

    OtpVerification generateOtp(String phone, OtpPurpose purpose);

    boolean verifyOtp(String phone, String otpCode, OtpPurpose purpose);
}
