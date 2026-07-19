package com.jcbbooking.controller;

import com.jcbbooking.dto.MenuRequest;
import com.jcbbooking.dto.MenuResponse;
import com.jcbbooking.model.Menu;
import com.jcbbooking.repository.MenuRepository;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.service.MenuService;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final MenuService menuService;
    private final MenuRepository menuRepository;

    // ─── Existing: accessible menus for logged-in user ────────────────────────
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

    // ─── Admin CRUD ────────────────────────────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Menu>>> getAllMenus() {
        log.info("REST request to fetch all menus (admin)");
        List<Menu> menus = menuRepository.findAllByOrderByDisplayOrderAsc();
        return ResponseEntity.ok(ApiResponse.success("All menus retrieved successfully", menus));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Menu>> saveMenu(@RequestBody MenuRequest request) {
        log.info("REST request to save/update menu: {}", request);

        boolean isNew = (request.getId() == null || request.getId() == 0);

        // Uniqueness check on menuCode for new menus
        if (isNew && menuRepository.existsByMenuCode(request.getMenuCode())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Menu code '" + request.getMenuCode() + "' already exists"));
        }

        Menu.MenuBuilder builder = Menu.builder()
                .menuName(request.getMenuName())
                .menuCode(request.getMenuCode())
                .routePath(request.getRoutePath())
                .icon(request.getIcon())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true);

        if (!isNew) {
            builder.id(request.getId());
        }

        // Resolve parent menu
        if (request.getParentMenuId() != null) {
            menuRepository.findById(request.getParentMenuId()).ifPresent(builder::parentMenu);
        }

        Menu saved = menuRepository.save(builder.build());
        String msg = isNew ? "Menu created successfully" : "Menu updated successfully";
        return ResponseEntity.ok(ApiResponse.success(msg, saved));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteMenu(@PathVariable Long id) {
        log.info("REST request to delete menu ID: {}", id);
        if (!menuRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Menu not found"));
        }
        menuRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Menu deleted successfully"));
    }
}
