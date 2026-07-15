package com.lionsclub.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionsclub.api.TestcontainersConfiguration;
import com.lionsclub.api.config.DataSeeder;
import com.lionsclub.api.infrastructure.persistence.EventRepository;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@ImportTestcontainers(TestcontainersConfiguration.class)
class SeedDataAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void setUp() throws Exception {
        eventRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
    }

    @Test
    void loginWithSeededAdminSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@lionsclub.com", "password": "admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().maxAge("auth_token", 900))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void loginWithSeededMemberSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "fatima@lionsclub.com", "password": "member123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().maxAge("auth_token", 900))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }
}
