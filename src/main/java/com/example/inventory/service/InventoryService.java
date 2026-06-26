package com.example.inventory.service;

import com.example.inventory.entity.InventoryHistory;
import com.example.inventory.entity.Product;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.repository.InventoryHistoryRepository;
import com.example.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryHistoryRepository historyRepository;

    @Transactional
    public void deductStock(Long productId, Integer quantity, String changeType) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock. Available: %d, Requested: %d",
                            product.getStockQuantity(), quantity)
            );
        }

        int oldQty = product.getStockQuantity();
        int newQty = oldQty - quantity;
        product.setStockQuantity(newQty);

        try {
            productRepository.save(product);
            log.info("Stock deducted for Product {}. Old: {}, New: {}", productId, oldQty, newQty);
            saveHistory(productId, changeType, -quantity, oldQty, newQty);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("Concurrent update on product {}", productId);
            throw new RuntimeException("Stock update failed due to concurrent modification. Please retry.", e);
        }
    }

    @Transactional
    public void addStock(Long productId, Integer quantity, String changeType) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        int oldQty = product.getStockQuantity();
        int newQty = oldQty + quantity;
        product.setStockQuantity(newQty);

        try {
            productRepository.save(product);
            saveHistory(productId, changeType, quantity, oldQty, newQty);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("Stock update failed due to concurrent modification.", e);
        }
    }

    private void saveHistory(Long productId, String changeType, int change, int oldQty, int newQty) {
        InventoryHistory history = new InventoryHistory();
        history.setProductId(productId);
        history.setChangeType(changeType);
        history.setQuantityChange(change);
        history.setOldQuantity(oldQty);
        history.setNewQuantity(newQty);
        historyRepository.save(history);
    }
}