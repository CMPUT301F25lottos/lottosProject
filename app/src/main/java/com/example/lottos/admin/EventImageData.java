package com.example.lottos.admin;

/**
 * A data model class that represents the essential information for an event's image.
 * This class is used to hold the data required for displaying and managing event posters
 * in the admin interface, such as in the {@code AllImagesAdapter}.
 */
public class EventImageData {
    /**
     * The unique identifier for the event, corresponding to the document ID in Firestore.
     */
    public final String eventId;
    /**
     * The public URL of the event's poster image stored in Firebase Storage.
     */
    public final String posterUrl;
    /**
     * The name of the event.
     */
    public final String eventName;
    /**
     * The username of the person who organized the event.
     */
    public final String organizerName;

    /**
     * Constructs a new EventImageData object.
     *
     * @param eventId       The unique ID of the event.
     * @param posterUrl     The URL of the event's poster.
     * @param eventName     The name of the event.
     * @param organizerName The name of the event's organizer.
     */
    public EventImageData(String eventId, String posterUrl, String eventName, String organizerName) {
        this.eventId = eventId;
        this.posterUrl = posterUrl;
        this.eventName = eventName;
        this.organizerName = organizerName;
    }
}
