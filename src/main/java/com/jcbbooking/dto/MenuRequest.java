package com.jcbbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuRequest {
    private Long id;
    private String menuName;
    private String menuCode;
    private Long parentMenuId;
    private String routePath;
    private String icon;
    private Integer displayOrder;
    private Boolean active;
}
