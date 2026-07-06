package com.lionsclub.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/lions_club",
    "spring.datasource.username=lions_club",
    "spring.datasource.password=lions_club_dev",
    "app.jwt.secret=test_secret"
})
@AutoConfigureMockMvc
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
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }
}
