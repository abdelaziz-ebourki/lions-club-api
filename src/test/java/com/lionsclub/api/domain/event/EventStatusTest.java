package com.lionsclub.api.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EventStatusTest {

    @Test
    void shouldHaveExactlyFourValues() {
        EventStatus[] values = EventStatus.values();
        assertThat(values).hasSize(4);
    }

    @Test
    void shouldContainDraft() {
        assertThat(EventStatus.valueOf("DRAFT")).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void shouldContainPublished() {
        assertThat(EventStatus.valueOf("PUBLISHED")).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void shouldContainCancelled() {
        assertThat(EventStatus.valueOf("CANCELLED")).isEqualTo(EventStatus.CANCELLED);
    }

    @Test
    void shouldContainCompleted() {
        assertThat(EventStatus.valueOf("COMPLETED")).isEqualTo(EventStatus.COMPLETED);
    }
}
