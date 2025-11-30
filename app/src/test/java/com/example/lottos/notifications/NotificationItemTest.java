package com.example.lottos.notifications;

import com.google.firebase.Timestamp;
import org.junit.Test;
import java.util.Date;import static org.junit.Assert.*;

/**
 * Unit tests for the NotificationItem data class.
 */
public class NotificationItemTest {

    @Test
    public void constructor_assignsFieldsCorrectly() {
        String expectedId = "notification-123";
        String expectedContent = "You have been selected for an event!";
        String expectedEventName = "Annual Tech Conference";
        String expectedReceiver = "receiver-uid-456";
        String expectedSender = "sender-uid-789";
        Timestamp expectedTimestamp = new Timestamp(new Date());

        NotificationItem notification = new NotificationItem(
                expectedId,
                expectedContent,
                expectedEventName,
                expectedReceiver,
                expectedSender,
                expectedTimestamp
        );

        assertEquals("The id should match the constructor argument", expectedId, notification.id);
        assertEquals("The content should match the constructor argument", expectedContent, notification.content);
        assertEquals("The eventName should match the constructor argument", expectedEventName, notification.eventName);
        assertEquals("The receiver should match the constructor argument", expectedReceiver, notification.receiver);
        assertEquals("The sender should match the constructor argument", expectedSender, notification.sender);
        assertEquals("The timestamp should match the constructor argument", expectedTimestamp, notification.timestamp);
    }
}
