package com.example.inventory.mapper;

import com.example.inventory.dto.ProductDTO;
import com.example.inventory.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductDTO toDto(Product product);
    Product toEntity(ProductDTO productDTO);
    void updateEntityFromDto(ProductDTO dto, @MappingTarget Product entity);
    List<ProductDTO> toDtoList(List<Product> products);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "deleted", ignore = true)
    Product toEntityFromCreateRequest(com.example.inventory.dto.CreateProductRequest request);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "deleted", ignore = true)
    @org.mapstruct.Mapping(target = "variants", ignore = true)
    void updateEntityFromUpdateRequest(com.example.inventory.dto.UpdateProductRequest request, @MappingTarget Product entity);
}
