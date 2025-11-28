package com.example.lottos.organizer;

import com.example.lottos.entities.Event;
import com.example.lottos.EventRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Handles organizer-side logic for creating/updating/deleting events
 * and linking them to the organizer user document.
 */
public class OrganizerEventManager {

    private final EventRepository repo = new EventRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();



    /** Creates a new event document + links it to the organizer. */
    public void createEvent(
            Event event,
            LocalDateTime registerEndTime,
            Integer waitListCapacity,
            Runnable onSuccess,
            EventRepository.OnError onError
    ) {

        String eventId = event.getEventId();

        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        map.put("eventName", event.getEventName());
        map.put("organizer", event.getOrganizer());
        map.put("organizerUid", FirebaseAuth.getInstance().getUid());
        map.put("description", event.getDescription());
        map.put("location", event.getLocation());
        map.put("selectionCap", event.getSelectionCap());
        map.put("IsOpen", event.getIsOpen());
        map.put("IsLottery", false);

        map.put("posterUrl", event.getPosterUrl());

        if (waitListCapacity != null) {
            map.put("waitListCapacity", waitListCapacity);
        }

        map.put("startTime", toTimestamp(event.getStartTime()));
        map.put("endTime", toTimestamp(event.getEndTime()));
        map.put("registerEndTime", toTimestamp(registerEndTime));

        map.put("createdAt", Timestamp.now());

        map.put("waitList", makeWaitListMap());
        map.put("selectedList", makeUserListMap());
        map.put("enrolledList", makeUserListMap());
        map.put("cancelledList", makeUserListMap());
        map.put("organizedList", makeUserListMap());
        map.put("notSelectedList", makeUserListMap());

        repo.createEvent(eventId, map, () -> {
            db.collection("users")
                    .document(event.getOrganizer())
                    .update("organizedEvents.events", FieldValue.arrayUnion(eventId))
                    .addOnSuccessListener(v -> onSuccess.run())
                    .addOnFailureListener(onError::run);

        }, onError);
    }

    public void updateEvent(String eventId, Map<String, Object> updates, Runnable onSuccess, EventRepository.OnError onError) {
        repo.updateEvent(eventId, updates, onSuccess, onError);
    }

    public void deleteEvent(String eventId, Runnable onSuccess, EventRepository.OnError onError) {
        repo.deleteEvent(eventId, onSuccess, onError);
    }

    private Timestamp toTimestamp(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        java.util.Date date =
                java.util.Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        return new Timestamp(date);
    }

    private Map<String, Object> makeWaitListMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("closeDate", "");
        m.put("closeTime", "");
        m.put("users", new ArrayList<String>());
        return m;
    }

    private Map<String, Object> makeUserListMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("users", new ArrayList<String>());
        return m;
    }
}
