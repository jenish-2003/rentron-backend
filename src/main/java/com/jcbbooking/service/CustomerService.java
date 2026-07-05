package com.jcbbooking.service;

import com.jcbbooking.model.User;

import java.util.List;

public interface CustomerService {

    List<User> getAllCustomers();

    User getCustomerById(Long id);

    User saveCustomer(User customer);

    void deleteCustomerById(Long id);

    void deleteCustomer(User customer);
}
