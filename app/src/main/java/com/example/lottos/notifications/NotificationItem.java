package com.example.lottos.notifications;

import com.google.firebase.Timestamp;

public class NotificationItem {

    public final String id;      // Document ID from Firestore
    public final String content;
    public final String eventName;
    public final String receiver;
    public final String sender;
    public final Timestamp timestamp;

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
