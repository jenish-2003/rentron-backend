package com.jcbbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuResponse {
    private Long id;
    private String menuName;
    private String menuCode;
    private String routePath;
    private String icon;
    private Integer displayOrder;
    
    @Builder.Default
    private List<MenuResponse> subMenus = new ArrayList<>();
}
