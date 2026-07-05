package com.jcbbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleAccessAssignmentResponse {
    private List<Long> assignedMenuIds;
    private List<Long> assignedPermissionIds;
}
