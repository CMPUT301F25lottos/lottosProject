
package com.example.lottos.admin;

public class EventImageData {
    public final String eventId;
    public final String posterUrl;
    public final String eventName;
    public final String organizerName;

    public EventImageData(String eventId, String posterUrl, String eventName, String organizerName) {
        this.eventId = eventId;
        this.posterUrl = posterUrl;
        this.eventName = eventName;
        this.organizerName = organizerName;
    }
}
