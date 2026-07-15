package com.lionsclub.api.domain.rsvp;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.user.User;
import jakarta.persistence.ManyToOne;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RsvpTest {

    @Test
    void shouldHaveIdField() throws NoSuchFieldException {
        Field idField = Rsvp.class.getDeclaredField("id");
        assertThat(idField.getType()).isEqualTo(UUID.class);
    }

    @Test
    void shouldHaveEventRelationship() throws NoSuchFieldException {
        Field eventField = Rsvp.class.getDeclaredField("event");
        assertThat(eventField.getType()).isEqualTo(Event.class);
        assertThat(eventField.getAnnotation(ManyToOne.class)).isNotNull();
    }

    @Test
    void shouldHaveMemberRelationship() throws NoSuchFieldException {
        Field memberField = Rsvp.class.getDeclaredField("member");
        assertThat(memberField.getType()).isEqualTo(User.class);
        assertThat(memberField.getAnnotation(ManyToOne.class)).isNotNull();
    }

    @Test
    void shouldHaveStatusField() throws NoSuchFieldException {
        Field statusField = Rsvp.class.getDeclaredField("status");
        assertThat(statusField.getType()).isEqualTo(RsvpStatus.class);
    }

    @Test
    void shouldHavePlusOneField() throws NoSuchFieldException {
        Field plusOneField = Rsvp.class.getDeclaredField("plusOne");
        assertThat(plusOneField.getType()).isEqualTo(int.class);
    }

    @Test
    void shouldHaveNotesField() throws NoSuchFieldException {
        Field notesField = Rsvp.class.getDeclaredField("notes");
        assertThat(notesField.getType()).isEqualTo(String.class);
    }

    @Test
    void shouldHaveCreatedAtField() throws NoSuchFieldException {
        Field createdAtField = Rsvp.class.getDeclaredField("createdAt");
        assertThat(createdAtField.getType()).isEqualTo(LocalDateTime.class);
    }

    @Test
    void shouldHaveUpdatedAtField() throws NoSuchFieldException {
        Field updatedAtField = Rsvp.class.getDeclaredField("updatedAt");
        assertThat(updatedAtField.getType()).isEqualTo(LocalDateTime.class);
    }

    @Test
    void shouldHaveRsvpStatusValues() {
        assertThat(RsvpStatus.values()).containsExactly(RsvpStatus.YES, RsvpStatus.NO, RsvpStatus.MAYBE);
    }
}
