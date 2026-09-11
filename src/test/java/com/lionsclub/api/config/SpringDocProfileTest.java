package com.lionsclub.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// NOTE: datasource intentionally NOT overridden here — this test must honor
// the same ambient DB as the rest of the suite (default localhost:5433,
// overridable via SPRING_DATASOURCE_URL). A hardcoded URL would silently
// ignore the override and couple the suite to one fixed port.
@SpringBootTest(properties = {
    "app.jwt.secret=test_secret"
})
@AutoConfigureMockMvc
@ImportTestcontainers(com.lionsclub.api.TestcontainersConfiguration.class)
@ActiveProfiles("prod")
@WithMockUser
class SpringDocProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void swaggerUiReturns404InProd() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isNotFound());
    }

    @Test
    void apiDocsReturns404InProd() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isNotFound());
    }
}
