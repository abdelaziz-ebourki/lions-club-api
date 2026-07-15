package com.lionsclub.api.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventTest {

    @Test
    void shouldHaveIdField() throws NoSuchFieldException {
        Field idField = Event.class.getDeclaredField("id");
        assertThat(idField.getType()).isEqualTo(UUID.class);
    }

    @Test
    void shouldHaveTitleField() throws NoSuchFieldException {
        Field titleField = Event.class.getDeclaredField("title");
        assertThat(titleField.getType()).isEqualTo(String.class);
    }

    @Test
    void shouldHaveStartDateTimeField() throws NoSuchFieldException {
        Field field = Event.class.getDeclaredField("startDateTime");
        assertThat(field.getType()).isEqualTo(LocalDateTime.class);
    }

    @Test
    void shouldHaveEndDateTimeField() throws NoSuchFieldException {
        Field field = Event.class.getDeclaredField("endDateTime");
        assertThat(field.getType()).isEqualTo(LocalDateTime.class);
    }

    @Test
    void shouldHaveStatusField() throws NoSuchFieldException {
        Field field = Event.class.getDeclaredField("status");
        assertThat(field.getType()).isEqualTo(EventStatus.class);
    }
}
