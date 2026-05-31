package com.jcbbooking.repository;

import com.jcbbooking.model.Role;
import com.jcbbooking.model.RolePermissionAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionAccessRepository extends JpaRepository<RolePermissionAccess, Long> {

    List<RolePermissionAccess> findByRole(Role role);
}
