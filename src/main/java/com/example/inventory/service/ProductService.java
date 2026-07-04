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

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public List<ProductDTO> getAllProducts() {
        return productMapper.toDtoList(productRepository.findByDeletedFalse());
    }

    public ProductDTO getProduct(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.getDeleted())
                .orElseThrow(() -> new NoSuchElementException("Product not found"));
        return productMapper.toDto(product);
    }

    public ProductDTO createProduct(ProductDTO dto) {
        if (productRepository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("SKU already exists");
        }
        Product product = productMapper.toEntity(dto);
        return productMapper.toDto(productRepository.save(product));
    }

    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.getDeleted())
                .orElseThrow(() -> new NoSuchElementException("Product not found"));
        
        productMapper.updateEntityFromDto(dto, product);
        return productMapper.toDto(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));
        product.setDeleted(true);
        productRepository.save(product);
    }
}
