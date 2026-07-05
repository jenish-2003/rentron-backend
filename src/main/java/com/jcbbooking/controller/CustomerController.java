package com.jcbbooking.controller;

import com.jcbbooking.model.User;
import com.jcbbooking.service.CustomerService;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllCustomers() {
        log.info("REST request to get all customers");
        List<User> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", customers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getCustomerById(@PathVariable Long id) {
        log.info("REST request to get customer by id: {}", id);
        User customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved successfully", customer));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<User>> saveCustomer(@RequestBody User customer) {
        log.info("REST request to save/update customer: {}", customer);
        User saved = customerService.saveCustomer(customer);
        String message = (customer.getId() == null || customer.getId() == 0)
                ? "Customer created successfully"
                : "Customer updated successfully";
        return ResponseEntity.ok(ApiResponse.success(message, saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomerById(@PathVariable Long id) {
        log.info("REST request to delete customer by id: {}", id);
        customerService.deleteCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@RequestBody User customer) {
        log.info("REST request to delete customer by entity body: {}", customer);
        customerService.deleteCustomer(customer);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully"));
    }
}
