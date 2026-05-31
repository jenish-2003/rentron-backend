package com.jcbbooking.service;

import com.jcbbooking.model.User;

import java.util.Optional;

public interface UserService {

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    User createUser(User user);
    
    User save(User user);
}
