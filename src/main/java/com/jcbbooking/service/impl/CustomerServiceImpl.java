package com.jcbbooking.service.impl;

import com.jcbbooking.exception.DuplicateResourceException;
import com.jcbbooking.exception.ResourceNotFoundException;
import com.jcbbooking.model.Role;
import com.jcbbooking.model.User;
import com.jcbbooking.repository.UserRepository;
import com.jcbbooking.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<User> getAllCustomers() {
        log.info("Fetching all customers with role CUSTOMER");
        return userRepository.findAllByRole(Role.CUSTOMER);
    }

    @Override
    public User getCustomerById(Long id) {
        log.info("Fetching customer by id: {}", id);
        return userRepository.findById(id)
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    @Override
    @Transactional
    public User saveCustomer(User customer) {
        log.info("Saving/updating customer entity: {}", customer);

        // Operation type: id = 0 or null means New
        if (customer.getId() == null || customer.getId() == 0) {
            log.info("Creating a new customer");
            customer.setId(null); // Let JPA handle ID generation
            customer.setRole(Role.CUSTOMER);

            // Validate unique phone and email
            if (userRepository.existsByPhone(customer.getPhone())) {
                throw new DuplicateResourceException("Phone number already registered: " + customer.getPhone());
            }

            if (customer.getEmail() != null && !customer.getEmail().trim().isEmpty()) {
                if (userRepository.existsByEmail(customer.getEmail())) {
                    throw new DuplicateResourceException("Email address already registered: " + customer.getEmail());
                }
            }

            // Encrypt password
            if (customer.getPasswordHash() != null && !customer.getPasswordHash().trim().isEmpty()) {
                customer.setPasswordHash(passwordEncoder.encode(customer.getPasswordHash()));
            } else {
                // Set default password if none is provided
                customer.setPasswordHash(passwordEncoder.encode("Password123"));
            }

            // Defaults
            if (customer.getVerified() == null) {
                customer.setVerified(true);
            }
            if (customer.getActive() == null) {
                customer.setActive(true);
            }
            if (customer.getTotalBookings() == null) {
                customer.setTotalBookings(0);
            }
            if (customer.getWalletBalance() == null) {
                customer.setWalletBalance(0.0);
            }
            if (customer.getCreatedAt() == null) {
                customer.setCreatedAt(java.time.LocalDateTime.now());
            }
            if (customer.getUpdatedAt() == null) {
                customer.setUpdatedAt(java.time.LocalDateTime.now());
            }

            return userRepository.save(customer);
        } else {
            // Operation type: Change (Edit)
            log.info("Updating existing customer with id: {}", customer.getId());
            User existing = userRepository.findById(customer.getId())
                    .filter(u -> u.getRole() == Role.CUSTOMER)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customer.getId()));

            // Validate uniqueness on change
            if (!existing.getPhone().equals(customer.getPhone()) && userRepository.existsByPhone(customer.getPhone())) {
                throw new DuplicateResourceException("Phone number already registered: " + customer.getPhone());
            }

            if (customer.getEmail() != null && !customer.getEmail().trim().isEmpty()) {
                if ((existing.getEmail() == null || !existing.getEmail().equals(customer.getEmail()))
                        && userRepository.existsByEmail(customer.getEmail())) {
                    throw new DuplicateResourceException("Email address already registered: " + customer.getEmail());
                }
            }

            // Update allowed fields
            existing.setFullName(customer.getFullName());
            existing.setPhone(customer.getPhone());
            existing.setEmail(customer.getEmail());
            existing.setUpdatedAt(java.time.LocalDateTime.now());

            if (customer.getActive() != null) {
                existing.setActive(customer.getActive());
            }
            if (customer.getVerified() != null) {
                existing.setVerified(customer.getVerified());
            }
            if (customer.getTotalBookings() != null) {
                existing.setTotalBookings(customer.getTotalBookings());
            }
            if (customer.getWalletBalance() != null) {
                existing.setWalletBalance(customer.getWalletBalance());
            }
            if (customer.getAddress() != null) {
                existing.setAddress(customer.getAddress());
            }

            // If a new password is provided, rehash and update it
            if (customer.getPasswordHash() != null && !customer.getPasswordHash().trim().isEmpty()
                    && !customer.getPasswordHash().equals(existing.getPasswordHash())) {
                existing.setPasswordHash(passwordEncoder.encode(customer.getPasswordHash()));
            }

            return userRepository.save(existing);
        }
    }

    @Override
    @Transactional
    public void deleteCustomerById(Long id) {
        log.info("Deleting customer by id: {}", id);
        User customer = userRepository.findById(id)
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        userRepository.delete(customer);
    }

    @Override
    @Transactional
    public void deleteCustomer(User customer) {
        if (customer == null || customer.getId() == null) {
            throw new IllegalArgumentException("Customer and customer ID must not be null for deletion");
        }
        log.info("Deleting customer entity with id: {}", customer.getId());
        User existing = userRepository.findById(customer.getId())
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customer.getId()));
        userRepository.delete(existing);
    }
}
