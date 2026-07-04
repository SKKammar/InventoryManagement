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
    
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    OrderItemDTO toDto(OrderItem orderItem);

    List<OrderDTO> toDtoList(List<Order> orders);
}
