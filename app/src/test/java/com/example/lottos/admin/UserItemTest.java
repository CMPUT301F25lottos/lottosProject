package com.example.lottos.admin;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the UserItem data class.
 */
public class UserItemTest {

    private UserItem userItem;
    private final String expectedUserId = "user-abc-123";
    private final String expectedUserName = "Jane Doe";

    @Before
    public void setUp() {
        userItem = new UserItem(expectedUserId, expectedUserName);
    }

    @Test
    public void constructor_shouldInitializeFieldsCorrectly() {
        assertEquals("User ID should be set by constructor", expectedUserId, userItem.getUserId());
        assertEquals("User Name should be set by constructor", expectedUserName, userItem.getUserName());
        assertEquals("Events participated should initialize to 0", 0, userItem.getEventsParticipated());
        assertEquals("Events created should initialize to 0", 0, userItem.getEventsCreated());
    }

    @Test
    public void setEventsParticipated_shouldUpdateValue() {
        int newParticipatedCount = 15;
        userItem.setEventsParticipated(newParticipatedCount);
        assertEquals("getEventsParticipated should return the updated value", newParticipatedCount, userItem.getEventsParticipated());
    }



    @Test
    public void setEventsCreated_shouldUpdateValue() {
        int newCreatedCount = 3;
        userItem.setEventsCreated(newCreatedCount);
        assertEquals("getEventsCreated should return the updated value", newCreatedCount, userItem.getEventsCreated());
    }
}
