package com.example.inventory.dto;

import lombok.Data;

@Data
public class WarehouseDTO {
    private Long id;
    private String code;
    private String name;
    private String location;
    private Integer priority;
    private Boolean active;
}
