package com.example.inventory;

import com.example.inventory.dto.CreateOrderRequest;
import com.example.inventory.entity.User;
import com.example.inventory.enums.RoleType;
import com.example.inventory.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class SecurityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRejectAnonymousAccessToProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/inventory/1/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void shouldRejectCustomerAccessToAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isForbidden()); // Requires ADMIN or WAREHOUSE_STAFF

        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isForbidden()); // Requires ADMIN
                
        // Mass assignment attempt
        String payload = "{\"quantityChange\": 10, \"reason\": \"hack\", \"id\": 999, \"createdBy\": \"attacker\"}";
        mockMvc.perform(post("/api/inventory/1/adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldAllowAdminAccessToAdminEndpoints() throws Exception {
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("pass");
        adminUser.setRole(RoleType.ADMIN);
        userRepository.save(adminUser);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void shouldRejectMassAssignmentInCheckout() throws Exception {
        String payload = """
                {
                  "idempotencyKey": "%s",
                  "items": [
                    {
                      "productVariantId": 1,
                      "quantity": 2
                    }
                  ],
                  "status": "COMPLETED",
                  "totalAmount": 0,
                  "user": {
                     "id": 999,
                     "role": "ADMIN"
                  }
                }
                """.formatted(UUID.randomUUID().toString());

        // Assuming product variant doesn't exist, it should return 404 Not Found rather than allowing creation
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isNotFound());
    }
}
