package com.jcbbooking.repository;

import com.jcbbooking.model.Role;
import com.jcbbooking.model.RoleMenuAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleMenuAccessRepository extends JpaRepository<RoleMenuAccess, Long> {

    List<RoleMenuAccess> findByRole(Role role);
    boolean existsByRoleAndMenu(Role role, com.jcbbooking.model.Menu menu);
}
