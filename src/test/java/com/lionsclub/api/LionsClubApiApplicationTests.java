package com.lionsclub.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

@SpringBootTest
@ImportTestcontainers(TestcontainersConfiguration.class)
class LionsClubApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
