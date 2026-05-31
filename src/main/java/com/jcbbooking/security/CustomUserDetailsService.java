package com.jcbbooking.security;

import com.jcbbooking.model.User;
import com.jcbbooking.repository.UserRepository;
import com.jcbbooking.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final MenuService menuService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;
        if (username.contains("@")) {
            user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        } else {
            user = userRepository.findByPhone(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + username));
        }

        List<String> permissions = menuService.getPermissionsForRole(user.getRole());
        return new CustomUserDetails(user, permissions);
    }
}
