package com.example.inventory;

import com.example.inventory.dto.CreateInventoryTransferRequest;
import com.example.inventory.dto.InventoryTransferDTO;
import com.example.inventory.entity.*;
import com.example.inventory.enums.RoleType;
import com.example.inventory.enums.TransactionType;
import com.example.inventory.repository.*;
import com.example.inventory.service.InventoryService;
import com.example.inventory.service.InventoryTransferService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InventoryTransferTests extends IntegrationTestBase {

    @Autowired
    private InventoryTransferService transferService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private InventoryTransferRepository transferRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InventoryTransactionRepository transactionRepository;

    private User testAdmin;
    private ProductVariant testVariant;
    private Warehouse whSource;
    private Warehouse whDest;
    private Inventory invSource;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        transferRepository.deleteAll();
        inventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();

        testAdmin = new User();
        testAdmin.setUsername("admin_transfer");
        testAdmin.setEmail("admin_tx@example.com");
        testAdmin.setPassword("password");
        testAdmin.setRole(RoleType.ADMIN);
        userRepository.save(testAdmin);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testAdmin.getUsername(), "password", List.of())
        );

        whSource = new Warehouse();
        whSource.setCode("WH-SRC");
        whSource.setName("Source WH");
        whSource.setPriority(1);
        warehouseRepository.save(whSource);

        whDest = new Warehouse();
        whDest.setCode("WH-DST");
        whDest.setName("Dest WH");
        whDest.setPriority(2);
        warehouseRepository.save(whDest);

        Product p = new Product();
        p.setName("Transfer Product");
        productRepository.save(p);

        testVariant = new ProductVariant();
        testVariant.setProduct(p);
        testVariant.setSku("SKU-TX-1");
        testVariant.setPrice(new BigDecimal("10.00"));
        productVariantRepository.save(testVariant);

        invSource = new Inventory();
        invSource.setProductVariant(testVariant);
        invSource.setWarehouse(whSource);
        invSource.setQuantityOnHand(0);
        invSource = inventoryRepository.save(invSource);

        inventoryService.adjustStock(invSource, 100, TransactionType.INITIAL_STOCK, "SYSTEM", null, "Initial", "SYSTEM");
    }

    @AfterEach
    void cleanup() {
        transactionRepository.deleteAll();
        transferRepository.deleteAll();
        inventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldTransferInventorySuccessfully() {
        CreateInventoryTransferRequest request = new CreateInventoryTransferRequest();
        request.setProductVariantId(testVariant.getId());
        request.setSourceWarehouseId(whSource.getId());
        request.setDestinationWarehouseId(whDest.getId());
        request.setQuantity(20);
        request.setReason("Rebalance");

        InventoryTransferDTO dto = transferService.transfer(request);

        assertThat(dto).isNotNull();
        assertThat(dto.getQuantity()).isEqualTo(20);

        Inventory updatedSource = inventoryRepository.findByProductVariantAndWarehouse(testVariant, whSource).orElseThrow();
        Inventory updatedDest = inventoryRepository.findByProductVariantAndWarehouse(testVariant, whDest).orElseThrow();

        assertThat(updatedSource.getQuantityOnHand()).isEqualTo(80); // 100 - 20
        assertThat(updatedDest.getQuantityOnHand()).isEqualTo(20);   // 0 + 20

        List<InventoryTransaction> transactions = transactionRepository.findAll();
        assertThat(transactions).hasSize(3); // 1 initial + 2 transfer

        boolean hasTransferOut = transactions.stream().anyMatch(t -> 
            t.getTransactionType() == TransactionType.TRANSFER_OUT && t.getQuantityChange() == -20);
        boolean hasTransferIn = transactions.stream().anyMatch(t -> 
            t.getTransactionType() == TransactionType.TRANSFER_IN && t.getQuantityChange() == 20);

        assertThat(hasTransferOut).isTrue();
        assertThat(hasTransferIn).isTrue();
    }

    @Test
    void shouldFailTransferWhenInsufficientStock() {
        CreateInventoryTransferRequest request = new CreateInventoryTransferRequest();
        request.setProductVariantId(testVariant.getId());
        request.setSourceWarehouseId(whSource.getId());
        request.setDestinationWarehouseId(whDest.getId());
        request.setQuantity(200); // Only 100 available
        request.setReason("Rebalance");

        assertThrows(IllegalArgumentException.class, () -> transferService.transfer(request));
    }
}
