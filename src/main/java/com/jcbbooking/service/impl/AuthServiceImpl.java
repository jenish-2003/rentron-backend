package com.jcbbooking.service.impl;

import com.jcbbooking.dto.*;
import com.jcbbooking.exception.AuthenticationException;
import com.jcbbooking.exception.OtpException;
import com.jcbbooking.exception.ResourceNotFoundException;
import com.jcbbooking.model.*;
import com.jcbbooking.repository.RefreshTokenRepository;
import com.jcbbooking.repository.UserRepository;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.security.jwt.JwtTokenProvider;
import com.jcbbooking.service.AuthService;
import com.jcbbooking.service.MenuService;
import com.jcbbooking.service.OtpService;
import com.jcbbooking.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final OtpService otpService;
    private final MenuService menuService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void sendOtp(SendOtpRequest request) {
        log.info("Processing send-otp request for phone: {}", request.getPhone());
        
        OtpPurpose purpose;
        try {
            purpose = OtpPurpose.valueOf(request.getPurpose().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new OtpException("Invalid OTP purpose: " + request.getPurpose());
        }

        // If purpose is RESET_PASSWORD, check if user exists
        if (purpose == OtpPurpose.RESET_PASSWORD) {
            userRepository.findByPhone(request.getPhone())
                    .orElseThrow(() -> new ResourceNotFoundException("No registered account found with phone: " + request.getPhone()));
        }

        otpService.generateOtp(request.getPhone(), purpose);
    }

    @Override
    @Transactional
    public AuthResponse verifyOtpLogin(VerifyOtpRequest request) {
        log.info("Processing verify-otp login request for phone: {}", request.getPhone());
        
        if (!"LOGIN".equalsIgnoreCase(request.getPurpose())) {
            throw new AuthenticationException("Invalid purpose for login verification: " + request.getPurpose());
        }

        // 1. Verify OTP code
        otpService.verifyOtp(request.getPhone(), request.getOtpCode(), OtpPurpose.LOGIN);

        // 2. Fetch User
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AuthenticationException("User registration incomplete for phone: " + request.getPhone()));

        // 3. Validate user status and build session
        return generateUserSession(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Processing unified login request for type: {}", request.getLoginType());
        
        // 1. Validate deviceType payload
        String deviceType = request.getDeviceType();
        if (deviceType == null || deviceType.trim().isEmpty()) {
            throw new AuthenticationException("Device type (deviceType: WEB or MOBILE) is required");
        }
        deviceType = deviceType.toUpperCase().trim();
        if (!deviceType.equals("WEB") && !deviceType.equals("MOBILE")) {
            throw new AuthenticationException("Invalid device type. Allowed values: WEB, MOBILE");
        }

        User user;
        String type = request.getLoginType().toUpperCase();

        switch (type) {
            case "PHONE_PASSWORD" -> {
                if (request.getPhone() == null || request.getPassword() == null) {
                    throw new AuthenticationException("Phone and password are required for PHONE_PASSWORD login");
                }
                user = userRepository.findByPhone(request.getPhone())
                        .orElseThrow(() -> new AuthenticationException("Invalid phone number or password"));
                
                if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                    throw new AuthenticationException("Invalid phone number or password");
                }
            }
            case "EMAIL_PASSWORD" -> {
                if (request.getEmail() == null || request.getPassword() == null) {
                    throw new AuthenticationException("Email and password are required for EMAIL_PASSWORD login");
                }
                user = userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new AuthenticationException("Invalid email address or password"));

                if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                    throw new AuthenticationException("Invalid email address or password");
                }
            }
            case "PHONE_OTP" -> {
                if (request.getPhone() == null || request.getOtp() == null) {
                    throw new AuthenticationException("Phone and OTP are required for PHONE_OTP login");
                }
                // Verify OTP
                otpService.verifyOtp(request.getPhone(), request.getOtp(), OtpPurpose.LOGIN);
                user = userRepository.findByPhone(request.getPhone())
                        .orElseThrow(() -> new AuthenticationException("User registration incomplete for phone: " + request.getPhone()));
            }
            default -> throw new AuthenticationException("Unsupported login type: " + request.getLoginType());
        }

        // 2. Enforce Device Type Restrictions based on User Role (Cross-Role Device Lockout)
        Role role = user.getRole();
        if (role == Role.ADMIN && !deviceType.equals("WEB")) {
            throw new AuthenticationException("Access denied: Administrator accounts are restricted to WEB client logins only");
        }
        if (role == Role.CONTRACTOR && !deviceType.equals("WEB")) {
            throw new AuthenticationException("Access denied: Contractor accounts are restricted to WEB client logins only");
        }
        if (role == Role.DRIVER && !deviceType.equals("MOBILE")) {
            throw new AuthenticationException("Access denied: Driver accounts are restricted to MOBILE app logins only");
        }

        return generateUserSession(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Processing refresh token request");
        
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthenticationException("Invalid or revoked refresh token"));

        if (refreshToken.getRevoked()) {
            throw new AuthenticationException("This refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new AuthenticationException("Refresh token has expired. Please log in again");
        }

        User user = refreshToken.getUser();
        
        // Ensure user is still active and valid
        validateUserStatus(user);

        // Delete old token (Token rotation policy)
        refreshTokenRepository.delete(refreshToken);

        return createSessionAndResponse(user);
    }

    @Override
    @Transactional
    public void logout(String refreshTokenValue) {
        log.info("Processing logout request");
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    log.info("Revoking refresh token for user ID: {}", token.getUser().getId());
                    refreshTokenRepository.delete(token);
                });
    }

    private AuthResponse generateUserSession(User user) {
        validateUserStatus(user);
        
        // Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return createSessionAndResponse(user);
    }

    private void validateUserStatus(User user) {
        if (!Boolean.TRUE.equals(user.getVerified())) {
            log.warn("Login blocked: User account is not verified. Phone: {}", user.getPhone());
            throw new AuthenticationException("Your account is not verified");
        }
        if (!Boolean.TRUE.equals(user.getActive())) {
            log.warn("Login blocked: User account is inactive. Phone: {}", user.getPhone());
            throw new AuthenticationException("Your account is currently suspended");
        }
    }

    private AuthResponse createSessionAndResponse(User user) {
        // Fetch permissions and menus
        List<String> permissions = menuService.getPermissionsForRole(user.getRole());
        List<MenuResponse> menus = menuService.getMenusForRole(user.getRole());

        // Construct UserDetails
        CustomUserDetails userDetails = new CustomUserDetails(user, permissions);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        
        // Rule: Refresh Token stored in DB, 100 years expiry (effectively never expires)
        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiryDate(LocalDateTime.now().plusYears(100))
                .revoked(false)
                .build();
        
        refreshTokenRepository.save(refreshToken);

        // Build response DTOs
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .verified(user.getVerified())
                .active(user.getActive())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .user(userDto)
                .role(user.getRole())
                .permissions(permissions)
                .menus(menus)
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing reset password request for phone: {}", request.getPhone());
        
        // 1. Verify OTP code
        otpService.verifyOtp(request.getPhone(), request.getOtpCode(), OtpPurpose.RESET_PASSWORD);
        
        // 2. Fetch User
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AuthenticationException("No registered account found with phone: " + request.getPhone()));
        
        // 3. Re-encrypt and update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        log.info("Password successfully reset for phone: {}", request.getPhone());
    }
}
