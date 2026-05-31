package com.jcbbooking.service;

import com.jcbbooking.dto.MenuResponse;
import com.jcbbooking.model.Role;

import java.util.List;

public interface MenuService {

    List<MenuResponse> getMenusForRole(Role role);

    List<String> getPermissionsForRole(Role role);
}
