package com.example.lottos;

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

    public void createEvent(
            Event event,
            LocalDateTime endRegisterTime,   // registration cutoff time
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

        if (waitListCapacity != null) {
            map.put("waitListCapacity", waitListCapacity);
        }

        map.put("startTime", new Timestamp(
                Date.from(event.getStartTime()
                        .atZone(ZoneId.systemDefault())
                        .toInstant())
        ));

        map.put("endTime", new Timestamp(
                Date.from(event.getEndTime()
                        .atZone(ZoneId.systemDefault())
                        .toInstant())
        ));

        map.put("EndRegisterTime", new Timestamp(
                Date.from(endRegisterTime
                        .atZone(ZoneId.systemDefault())
                        .toInstant())
        ));

        map.put("createdAt", Timestamp.now());

        // nested lists
        map.put("waitList", makeWaitListMap());
        map.put("selectedList", makeEmptyUserListMap());
        map.put("enrolledList", makeEmptyUserListMap());
        map.put("cancelledList", makeEmptyUserListMap());
        map.put("organizedList", makeEmptyUserListMap());
        map.put("notSelectedList", makeEmptyUserListMap());

        repo.createEvent(eventId, map, () -> {
            // link event to organizer document
            db.collection("users")
                    .document(event.getOrganizer())
                    .update("organizedEvents.events", FieldValue.arrayUnion(eventId))
                    .addOnSuccessListener(v -> onSuccess.run())
                    .addOnFailureListener(onError::run);
        }, onError);
    }

    public void updateEvent(
            String eventId,
            Map<String, Object> updates,
            Runnable onSuccess,
            EventRepository.OnError onError
    ) {
        repo.updateEvent(eventId, updates, onSuccess, onError);
    }

    public void deleteEvent(
            String eventId,
            Runnable onSuccess,
            EventRepository.OnError onError
    ) {
        repo.deleteEvent(eventId, onSuccess, onError);
    }

    private Map<String, Object> makeWaitListMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("closeDate", "");
        m.put("CloseTime", "");
        m.put("users", new ArrayList<String>());
        return m;
    }

    private Map<String, Object> makeEmptyUserListMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("users", new ArrayList<String>());
        return m;
    }
}
