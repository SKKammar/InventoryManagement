package com.example.inventory.mapper;

import com.example.inventory.dto.response.OrderDTO;
import com.example.inventory.entity.Order;
import com.example.inventory.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    @Mapping(target = "items", expression = "java(mapItems(order.getItems()))")
    OrderDTO toDto(Order order);

    default List<OrderDTO.OrderItemDTO> mapItems(List<OrderItem> items) {
        if (items == null) return List.of();
        return items.stream().map(item -> {
            OrderDTO.OrderItemDTO dto = new OrderDTO.OrderItemDTO();
            dto.setProductId(item.getProductId());
            dto.setQuantity(item.getQuantity());
            dto.setPrice(item.getPrice());
            return dto;
        }).collect(Collectors.toList());
    }
}