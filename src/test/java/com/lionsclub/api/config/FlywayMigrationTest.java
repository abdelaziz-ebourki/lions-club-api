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
class FlywayMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void v1MigrationAppliedSuccessfully() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT version, description, success FROM flyway_schema_history " +
                    "WHERE version = '1' ORDER BY installed_rank DESC LIMIT 1"
            );
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("version")).isEqualTo("1");
            assertThat(rs.getBoolean("success")).isTrue();
        }
    }

    @Test
    void v2MigrationAppliedSuccessfully() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT version, description, success FROM flyway_schema_history " +
                    "WHERE version = '2' ORDER BY installed_rank DESC LIMIT 1"
            );
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("version")).isEqualTo("2");
            assertThat(rs.getBoolean("success")).isTrue();
        }
    }

    @Test
    void v3MigrationAppliedSuccessfully() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT version, description, success FROM flyway_schema_history " +
                    "WHERE version = '3' ORDER BY installed_rank DESC LIMIT 1"
            );
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("version")).isEqualTo("3");
            assertThat(rs.getBoolean("success")).isTrue();
        }
    }

    @Test
    void v4MigrationAppliedSuccessfully() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT version, description, success FROM flyway_schema_history " +
                    "WHERE version = '4' ORDER BY installed_rank DESC LIMIT 1"
            );
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("version")).isEqualTo("4");
            assertThat(rs.getBoolean("success")).isTrue();
        }
    }

    @Test
    void rsvpTableExistsWithCorrectSchema() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT column_name, data_type, is_nullable FROM information_schema.columns " +
                    "WHERE table_name = 'rsvps' ORDER BY ordinal_position"
            );
            assertThat(rs.next()).isTrue();
            // Verify key columns exist
            boolean hasId = false, hasEventId = false, hasMemberId = false, hasStatus = false, hasPlusOne = false, hasNotes = false;
            do {
                String colName = rs.getString("column_name");
                switch (colName) {
                    case "id" -> hasId = true;
                    case "event_id" -> hasEventId = true;
                    case "member_id" -> hasMemberId = true;
                    case "status" -> hasStatus = true;
                    case "plus_one" -> hasPlusOne = true;
                    case "notes" -> hasNotes = true;
                }
            } while (rs.next());
            assertThat(hasId).isTrue();
            assertThat(hasEventId).isTrue();
            assertThat(hasMemberId).isTrue();
            assertThat(hasStatus).isTrue();
            assertThat(hasPlusOne).isTrue();
            assertThat(hasNotes).isTrue();
        }
    }

    @Test
    void rsvpUniqueConstraintExists() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT constraint_name FROM information_schema.table_constraints " +
                    "WHERE table_name = 'rsvps' AND constraint_type = 'UNIQUE' " +
                    "AND constraint_name LIKE '%event_id%member_id%'"
            );
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void rsvpIndexesExist() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT indexname FROM pg_indexes WHERE tablename = 'rsvps' " +
                    "AND indexname LIKE 'idx_rsvps_%'"
            );
            boolean hasEventStatus = false;
            boolean hasMember = false;
            while (rs.next()) {
                String indexName = rs.getString("indexname");
                if (indexName.contains("event_status")) hasEventStatus = true;
                if (indexName.contains("member")) hasMember = true;
            }
            assertThat(hasEventStatus).isTrue();
            assertThat(hasMember).isTrue();
        }
    }
}