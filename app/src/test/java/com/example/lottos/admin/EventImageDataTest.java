package com.example.lottos.admin;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the EventImageData data class.
 */
public class EventImageDataTest {

    @Test
    public void constructor_assignsFieldsCorrectly() {
        String expectedEventId = "event-123";
        String expectedPosterUrl = "https://example.com/poster.jpg";
        String expectedEventName = "The Grand Gala";
        String expectedOrganizerName = "John Doe";

        EventImageData imageData = new EventImageData(
                expectedEventId,
                expectedPosterUrl,
                expectedEventName,
                expectedOrganizerName
        );

        assertEquals("The eventId should match the constructor argument", expectedEventId, imageData.eventId);
        assertEquals("The posterUrl should match the constructor argument", expectedPosterUrl, imageData.posterUrl);
        assertEquals("The eventName should match the constructor argument", expectedEventName, imageData.eventName);
        assertEquals("The organizerName should match the constructor argument", expectedOrganizerName, imageData.organizerName);
    }
}
