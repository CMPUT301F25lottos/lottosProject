package com.example.lottos.notifications;

import com.google.firebase.Timestamp;

/**
 * A data class representing a single notification record from Firestore.
 *
 * Role: This class acts as a plain old Java object (POJO) to model the data
 * for one notification. It holds immutable fields for the notification's unique ID,
 * content, associated event, sender, receiver, and the time it was created.
 * This structure makes it easy to pass notification data between the data layer
 * (repository) and the UI layer (adapter/fragment).
 */
public class NotificationItem {

    public final String id;
    public final String content;
    public final String eventName;
    public final String receiver;
    public final String sender;
    public final Timestamp timestamp;

    /**
     * Constructs a new, immutable NotificationItem.
     *
     * @param id The unique document ID of the notification from Firestore.
     * @param content The main text message of the notification.
     * @param eventName The name of the event this notification is related to.
     * @param receiver The username of the person who will receive the notification.
     * @param sender The username of the person who sent the notification.
     * @param timestamp The Firebase Timestamp indicating when the notification was created.
     */
    public NotificationItem(String id,
                            String content,
                            String eventName,
                            String receiver,
                            String sender,
                            Timestamp timestamp) {
        this.id = id;
        this.content = content;
        this.eventName = eventName;
        this.receiver = receiver;
        this.sender = sender;
        this.timestamp = timestamp;
    }
}
