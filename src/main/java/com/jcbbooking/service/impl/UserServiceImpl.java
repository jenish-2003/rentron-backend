package com.jcbbooking.service.impl;

import com.jcbbooking.exception.DuplicateResourceException;
import com.jcbbooking.model.User;
import com.jcbbooking.repository.UserRepository;
import com.jcbbooking.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findByPhone(String phone) {
        log.info("Finding user by phone: {}", phone);
        return userRepository.findByPhone(phone);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        log.info("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public User createUser(User user) {
        log.info("Registering user with phone: {} and email: {}", user.getPhone(), user.getEmail());
        
        if (existsByPhone(user.getPhone())) {
            throw new DuplicateResourceException("Phone number already registered: " + user.getPhone());
        }

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty() && existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email address already registered: " + user.getEmail());
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User save(User user) {
        log.info("Updating user details for ID: {}", user.getId());
        return userRepository.save(user);
    }
}
