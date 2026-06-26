package com.example.inventory.mapper;

import com.example.inventory.dto.response.ProductDTO;
import com.example.inventory.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    @Mapping(source = "stockQuantity", target = "stock")
    ProductDTO toDto(Product product);

    @Mapping(source = "stock", target = "stockQuantity")
    Product toEntity(ProductDTO productDTO);
}