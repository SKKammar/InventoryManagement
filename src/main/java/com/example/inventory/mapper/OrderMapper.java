package com.example.inventory.mapper;

import com.example.inventory.dto.OrderDTO;
import com.example.inventory.dto.OrderItemDTO;
import com.example.inventory.entity.Order;
import com.example.inventory.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "user.username", target = "username")
    OrderDTO toDto(Order order);
    
    @Mapping(source = "productVariant.id", target = "productVariantId")
    @Mapping(source = "productVariant.sku", target = "sku")
    OrderItemDTO toDto(OrderItem orderItem);

    List<OrderDTO> toDtoList(List<Order> orders);

    @Mapping(source = "warehouse.id", target = "warehouseId")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    com.example.inventory.dto.OrderItemAllocationDTO toDto(com.example.inventory.entity.OrderItemAllocation allocation);
}
