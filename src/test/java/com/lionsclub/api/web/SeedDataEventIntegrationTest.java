package com.lionsclub.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@ImportTestcontainers(TestcontainersConfiguration.class)
class SeedDataEventIntegrationTest {

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
    void listEventsReturnsSeededUpcomingEvents() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Annual Charity Gala 2026')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'Community Clean-Up Day')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'Health & Wellness Workshop')]").exists());
    }

    @Test
    void listEventsReturnsSeededPastEvents() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Sight Screening Camp')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'Youth Leadership Summit')]").exists());
    }

    @Test
    void seededEventFieldsMatchMockData() throws Exception {
        mockMvc.perform(get("/api/events?status=upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Annual Charity Gala 2026')].description")
                        .value("Join us for an elegant evening of dinner, auctions, and entertainment to raise funds for local education initiatives."))
                .andExpect(jsonPath("$[?(@.title == 'Annual Charity Gala 2026')].location")
                        .value("Hyatt Regency Casablanca"))
                .andExpect(jsonPath("$[?(@.title == 'Annual Charity Gala 2026')].category")
                        .value("FUNDRAISER"))
                .andExpect(jsonPath("$[?(@.title == 'Annual Charity Gala 2026')].status")
                        .value("upcoming"));
    }
}
