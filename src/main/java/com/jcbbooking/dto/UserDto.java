package com.jcbbooking.dto;

import com.jcbbooking.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private Role role;
    private Boolean verified;
    private Boolean active;
}
