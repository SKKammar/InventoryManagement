package com.example.inventory.service;

import com.example.inventory.entity.InventoryHistory;
import com.example.inventory.entity.Product;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.repository.InventoryHistoryRepository;
import com.example.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryHistoryRepository historyRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setSku("TEST-001");
        product.setName("Test Product");
        product.setStockQuantity(100);
        product.setVersion(0L);
    }

    @Test
    void deductStock_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        inventoryService.deductStock(1L, 10, "ORDER_PLACED");

        assertEquals(90, product.getStockQuantity());
        verify(productRepository).save(product);
        verify(historyRepository).save(any(InventoryHistory.class));
    }

    @Test
    void deductStock_InsufficientStock_ThrowsException() {
        product.setStockQuantity(5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class, () ->
                inventoryService.deductStock(1L, 10, "ORDER_PLACED")
        );
        verify(productRepository, never()).save(any());
    }

    @Test
    void deductStock_ProductNotFound_ThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                inventoryService.deductStock(999L, 10, "ORDER_PLACED")
        );
    }

    @Test
    void deductStock_ConcurrentUpdate_ThrowsRuntimeException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class)))
                .thenThrow(ObjectOptimisticLockingFailureException.class);

        assertThrows(RuntimeException.class, () ->
                inventoryService.deductStock(1L, 10, "ORDER_PLACED")
        );
    }

    @Test
    void addStock_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        inventoryService.addStock(1L, 20, "STOCK_ADJUSTED");

        assertEquals(120, product.getStockQuantity());
        verify(historyRepository).save(any(InventoryHistory.class));
    }
}