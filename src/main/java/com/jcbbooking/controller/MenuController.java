package com.jcbbooking.controller;

import com.jcbbooking.dto.MenuResponse;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.service.MenuService;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getAccessibleMenus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to fetch accessible menus for user ID: {}", userDetails.getId());
        List<MenuResponse> menus = menuService.getMenusForRole(userDetails.getUser().getRole());
        return ResponseEntity.ok(ApiResponse.success("Menus retrieved successfully", menus));
    }

    @GetMapping("/privileges")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPrivileges(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to fetch accessible roles & privileges for user ID: {}", userDetails.getId());
        
        List<String> permissions = menuService.getPermissionsForRole(userDetails.getUser().getRole());
        List<MenuResponse> menus = menuService.getMenusForRole(userDetails.getUser().getRole());

        Map<String, Object> privileges = new HashMap<>();
        privileges.put("role", userDetails.getUser().getRole());
        privileges.put("permissions", permissions);
        privileges.put("menus", menus);

        return ResponseEntity.ok(ApiResponse.success("Privileges retrieved successfully", privileges));
    }
}
