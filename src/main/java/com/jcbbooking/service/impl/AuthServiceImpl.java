package com.jcbbooking.service.impl;

import com.jcbbooking.dto.*;
import com.jcbbooking.exception.AuthenticationException;
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
            throw new AuthenticationException("Invalid OTP purpose: " + request.getPurpose());
        }

        // If purpose is LOGIN or RESET_PASSWORD, check if user exists
        if (purpose == OtpPurpose.LOGIN || purpose == OtpPurpose.RESET_PASSWORD) {
            userRepository.findByPhone(request.getPhone())
                    .orElseThrow(() -> new AuthenticationException("No registered account found with phone: " + request.getPhone()));
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
        // Rule 3 & 4: DRIVER and CONTRACTOR cannot login unless verified = true and active = true
        if (user.getRole() == Role.DRIVER || user.getRole() == Role.CONTRACTOR) {
            if (!Boolean.TRUE.equals(user.getVerified())) {
                log.warn("Login blocked: {} account is not verified. Phone: {}", user.getRole(), user.getPhone());
                throw new AuthenticationException("Your account is pending admin approval and verification");
            }
            if (!Boolean.TRUE.equals(user.getActive())) {
                log.warn("Login blocked: {} account is inactive. Phone: {}", user.getRole(), user.getPhone());
                throw new AuthenticationException("Your account has been deactivated. Please contact support");
            }
        } else {
            // ADMIN and CUSTOMER only check active status
            if (!Boolean.TRUE.equals(user.getActive())) {
                log.warn("Login blocked: User account is inactive. Phone: {}", user.getPhone());
                throw new AuthenticationException("Your account is currently suspended");
            }
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
        
        // Rule: Refresh Token stored in DB, 7 days expiry
        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiryDate(LocalDateTime.now().plusDays(7)) // 7 days expiry
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
}
