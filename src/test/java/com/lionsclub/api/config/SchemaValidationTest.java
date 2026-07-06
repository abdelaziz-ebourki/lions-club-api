package com.lionsclub.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class SchemaValidationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void usersTableHasExpectedColumns() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT column_name, data_type, is_nullable " +
                "FROM information_schema.columns " +
                "WHERE table_name = 'users' " +
                "ORDER BY ordinal_position"
            );

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("column_name")).isEqualTo("id");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("column_name")).isEqualTo("email");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("column_name")).isEqualTo("password_hash");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("column_name")).isEqualTo("first_name");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("column_name")).isEqualTo("last_name");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("column_name")).isEqualTo("role");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("column_name")).isEqualTo("enabled");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("column_name")).isEqualTo("created_at");

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("column_name")).isEqualTo("updated_at");

            assertThat(rs.next()).isFalse();
        }
    }
}
