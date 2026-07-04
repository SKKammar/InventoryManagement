package com.example.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sku;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private Integer stockQuantity = 0;
    
    private String category;
    
    private Boolean deleted = false;

    @Version
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { 
        createdAt = LocalDateTime.now(); 
        updatedAt = LocalDateTime.now(); 
    }
    
    @PreUpdate
    protected void onUpdate() { 
        updatedAt = LocalDateTime.now(); 
    }
}
