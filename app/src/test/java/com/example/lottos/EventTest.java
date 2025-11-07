package com.example.lottos;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Event class.
 */
@RunWith(MockitoJUnitRunner.class)
public class EventTest {

    private Event event;
    private final String eventName = "Test Event";
    private final String organizerId = "organizer123";
    private final LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 10, 0);
    private final LocalDateTime endTime = LocalDateTime.of(2025, 1, 1, 12, 0);
    private final String description = "This is a test event.";
    private final String location = "Test Location";
    private final int selectionCap = 50;
    private final LocalDateTime endRegisterTime = LocalDateTime.of(2024, 12, 31, 23, 59);

    @Mock
    private FirebaseUser mockFirebaseUser;

    @Before
    public void setUp() {
        event = new Event(eventName, organizerId, startTime, endTime, description, location, selectionCap, endRegisterTime);
    }

    @Test
    public void testConstructor() {
        assertNotNull(event.getEventId());
        assertEquals(eventName, event.getEventName());
        assertEquals(organizerId, event.getOrganizer());
        assertEquals(startTime, event.getStartTime());
        assertEquals(endTime, event.getEndTime());
        assertEquals(description, event.getDescription());
        assertEquals(location, event.getLocation());
        assertEquals(selectionCap, event.getSelectionCap());
        assertTrue(event.getIsOpen());
        assertNotNull(event.getWaitList());
        assertNotNull(event.getSelectedList());
        assertNotNull(event.getCancelledList());
        assertNotNull(event.getEnrolledList());
    }

    @Test
    public void testIsOrganizer_whenUserIsOrganizer_returnsTrue() {
        try (MockedStatic<FirebaseAuth> mockedAuth = Mockito.mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockFirebaseAuth = Mockito.mock(FirebaseAuth.class);
            mockedAuth.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);
            when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
            when(mockFirebaseUser.getUid()).thenReturn(organizerId);

            assertTrue(event.isOrganizer());
        }
    }

    @Test
    public void testIsOrganizer_whenUserIsNotOrganizer_returnsFalse() {
        try (MockedStatic<FirebaseAuth> mockedAuth = Mockito.mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockFirebaseAuth = Mockito.mock(FirebaseAuth.class);
            mockedAuth.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);
            when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
            when(mockFirebaseUser.getUid()).thenReturn("anotherUser123");

            assertFalse(event.isOrganizer());
        }
    }

    @Test
    public void testIsOrganizer_whenNoUserLoggedIn_returnsFalse() {
        try (MockedStatic<FirebaseAuth> mockedAuth = Mockito.mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockFirebaseAuth = Mockito.mock(FirebaseAuth.class);
            mockedAuth.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);
            when(mockFirebaseAuth.getCurrentUser()).thenReturn(null);

            assertFalse(event.isOrganizer());
        }
    }

    @Test
    public void testGettersAndSetters() {
        // Test Setters
        event.setEventName("New Event Name");
        assertEquals("New Event Name", event.getEventName());

        event.setOrganizer("newOrganizer456");
        assertEquals("newOrganizer456", event.getOrganizer());

        LocalDateTime newStartTime = LocalDateTime.now().plusDays(2);
        event.setStartTime(newStartTime);
        assertEquals(newStartTime, event.getStartTime());

        LocalDateTime newEndTime = LocalDateTime.now().plusDays(2).plusHours(3);
        event.setEndTime(newEndTime);
        assertEquals(newEndTime, event.getEndTime());

        event.setDescription("New Description");
        assertEquals("New Description", event.getDescription());

        event.setLocation("New Location");
        assertEquals("New Location", event.getLocation());

        event.setSelectionCap(100);
        assertEquals(100, event.getSelectionCap());
    }

    @Test
    public void testEquals() {
        Event sameEvent = new Event(eventName, "someOtherOrganizer", startTime, endTime, description, location, selectionCap, endRegisterTime);
        Event differentEvent = new Event("Different Event", organizerId, startTime, endTime, description, location, selectionCap, endRegisterTime);
        Event nullEvent = null;
        Object otherObject = new Object();

        assertTrue(event.equals(event));
        assertTrue(event.equals(sameEvent));
        assertFalse(event.equals(differentEvent));
        assertFalse(event.equals(nullEvent));
        assertFalse(event.equals(otherObject));
    }

    @Test
    public void testHashCode() {
        Event sameEvent = new Event(eventName, "anotherOrganizer", startTime, endTime, description, location, selectionCap, endRegisterTime);
        assertEquals(event.hashCode(), sameEvent.hashCode());
    }
}
