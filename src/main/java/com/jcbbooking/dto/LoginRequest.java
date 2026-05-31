package com.jcbbooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Login type is required")
    private String loginType; // PHONE_PASSWORD, EMAIL_PASSWORD, PHONE_OTP

    private String email;
    
    private String phone;
    
    private String password;
    
    private String otp;

    private String deviceType; // WEB, MOBILE
}
