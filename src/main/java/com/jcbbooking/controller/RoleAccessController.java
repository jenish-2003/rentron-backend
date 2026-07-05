package com.jcbbooking.controller;

import com.jcbbooking.dto.RoleAccessAssignmentRequest;
import com.jcbbooking.dto.RoleAccessAssignmentResponse;
import com.jcbbooking.model.*;
import com.jcbbooking.repository.MenuRepository;
import com.jcbbooking.repository.PermissionRepository;
import com.jcbbooking.repository.RoleMenuAccessRepository;
import com.jcbbooking.repository.RolePermissionAccessRepository;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/role-access")
@RequiredArgsConstructor
@Slf4j
public class RoleAccessController {

    private final MenuRepository menuRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMenuAccessRepository roleMenuAccessRepository;
    private final RolePermissionAccessRepository rolePermissionAccessRepository;

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<Role>>> getRoles() {
        log.info("REST request to fetch all available roles");
        List<Role> roles = Arrays.asList(Role.values());
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roles));
    }

    @GetMapping("/menus")
    public ResponseEntity<ApiResponse<List<Menu>>> getAllMenus() {
        log.info("REST request to fetch all menus");
        List<Menu> menus = menuRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Menus retrieved successfully", menus));
    }

    @GetMapping("/assignments/{role}")
    public ResponseEntity<ApiResponse<RoleAccessAssignmentResponse>> getRoleAssignments(@PathVariable Role role) {
        log.info("REST request to fetch security assignments for role: {}", role);

        List<Long> assignedMenuIds = roleMenuAccessRepository.findByRole(role).stream()
                .map(rma -> rma.getMenu().getId())
                .collect(Collectors.toList());

        List<Long> assignedPermissionIds = rolePermissionAccessRepository.findByRole(role).stream()
                .map(rpa -> rpa.getPermission().getId())
                .collect(Collectors.toList());

        RoleAccessAssignmentResponse response = RoleAccessAssignmentResponse.builder()
                .assignedMenuIds(assignedMenuIds)
                .assignedPermissionIds(assignedPermissionIds)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Role assignments fetched successfully", response));
    }

    @PostMapping("/assignments/{role}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> saveRoleAssignments(
            @PathVariable Role role,
            @RequestBody RoleAccessAssignmentRequest request) {
        log.info("REST request to update security assignments for role: {}, menuIds: {}, permissionIds: {}",
                role, request.getMenuIds(), request.getPermissionIds());

        // 1. Clear previous Menu mappings
        List<RoleMenuAccess> existingMenus = roleMenuAccessRepository.findByRole(role);
        if (!existingMenus.isEmpty()) {
            roleMenuAccessRepository.deleteAllInBatch(existingMenus);
        }

        // 2. Clear previous Permission mappings
        List<RolePermissionAccess> existingPermissions = rolePermissionAccessRepository.findByRole(role);
        if (!existingPermissions.isEmpty()) {
            rolePermissionAccessRepository.deleteAllInBatch(existingPermissions);
        }

        // 3. Insert new Menu mappings
        if (request.getMenuIds() != null && !request.getMenuIds().isEmpty()) {
            List<RoleMenuAccess> newMenus = new ArrayList<>();
            for (Long menuId : request.getMenuIds()) {
                menuRepository.findById(menuId).ifPresent(menu -> {
                    newMenus.add(RoleMenuAccess.builder()
                            .role(role)
                            .menu(menu)
                            .build());
                });
            }
            if (!newMenus.isEmpty()) {
                roleMenuAccessRepository.saveAll(newMenus);
            }
        }

        // 4. Insert new Permission mappings
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<RolePermissionAccess> newPermissions = new ArrayList<>();
            for (Long permissionId : request.getPermissionIds()) {
                permissionRepository.findById(permissionId).ifPresent(permission -> {
                    newPermissions.add(RolePermissionAccess.builder()
                            .role(role)
                            .permission(permission)
                            .build());
                });
            }
            if (!newPermissions.isEmpty()) {
                rolePermissionAccessRepository.saveAll(newPermissions);
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Role assignments updated successfully"));
    }
}
