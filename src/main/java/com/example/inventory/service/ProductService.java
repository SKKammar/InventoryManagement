package com.example.inventory.service;

import com.example.inventory.dto.ProductDTO;
import com.example.inventory.entity.Product;
import com.example.inventory.mapper.ProductMapper;
import com.example.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final AuditService auditService;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper, AuditService auditService) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.auditService = auditService;
    }

    public List<ProductDTO> getAllProducts() {
        return productMapper.toDtoList(new java.util.ArrayList<>(productRepository.findAllWithVariantsAndInventory()));
    }

    public ProductDTO getProduct(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.getDeleted())
                .orElseThrow(() -> new NoSuchElementException("Product not found"));
        return productMapper.toDto(product);
    }

    public ProductDTO createProduct(com.example.inventory.dto.CreateProductRequest request) {
        Product product = productMapper.toEntityFromCreateRequest(request);
        if (product.getVariants() != null) {
            product.getVariants().forEach(v -> {
                v.setProduct(product);
                v.setActive(true);
            });
        }
        product = productRepository.save(product);
        
        auditService.logAction(com.example.inventory.enums.AuditAction.PRODUCT_CREATED, "PRODUCT", product.getId().toString(), null, java.util.Map.of("name", product.getName()));
        
        return productMapper.toDto(product);
    }

    public ProductDTO updateProduct(Long id, com.example.inventory.dto.UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.getDeleted())
                .orElseThrow(() -> new NoSuchElementException("Product not found"));
        
        productMapper.updateEntityFromUpdateRequest(request, product);
        product = productRepository.save(product);
        
        auditService.logAction(com.example.inventory.enums.AuditAction.PRODUCT_UPDATED, "PRODUCT", product.getId().toString(), null, null);
        
        return productMapper.toDto(product);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));
        product.setDeleted(true);
        productRepository.save(product);
        
        auditService.logAction(com.example.inventory.enums.AuditAction.PRODUCT_DEACTIVATED, "PRODUCT", product.getId().toString(), null, null);
    }
}
