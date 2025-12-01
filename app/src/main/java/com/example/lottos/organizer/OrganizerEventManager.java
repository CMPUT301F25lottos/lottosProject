package com.example.lottos.organizer;

import com.example.lottos.EventRepository;
import com.example.lottos.entities.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrganizerEventManager {

    private final EventRepository repo;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    /**
     * ✅ No-arg constructor – used by your Fragments:
     *   new OrganizerEventManager()
     */
    public OrganizerEventManager() {
        // If your EventRepository needs a db, wire it here
        FirebaseFirestore dbInstance = FirebaseFirestore.getInstance();
        this.db = dbInstance;
        this.repo = new EventRepository(dbInstance); // or new EventRepository() if that exists
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * ✅ Full constructor – for dependency injection / testing
     */
    public OrganizerEventManager(EventRepository repo,
                                 FirebaseFirestore db,
                                 FirebaseAuth auth) {
        this.repo = repo;
        this.db = db;
        this.auth = auth;
    }

    // ---------------- core methods below ----------------

    public void createEvent(
            Event event,
            LocalDateTime registerEndTime,
            Integer waitListCapacity,
            List<String> filterWords,
            Runnable onSuccess,
            EventRepository.OnError onError
    ) {

        String eventId = event.getEventId();

        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        map.put("eventName", event.getEventName());
        map.put("organizer", event.getOrganizer());
        map.put("organizerUid", auth.getUid());
        map.put("description", event.getDescription());
        map.put("location", event.getLocation());
        map.put("selectionCap", event.getSelectionCap());
        map.put("IsOpen", event.getIsOpen());
        map.put("IsLottery", false);

        map.put("posterUrl", event.getPosterUrl());

        if (waitListCapacity != null) {
            map.put("waitListCapacity", waitListCapacity);
        }

        // ✅ store filterWords array in Firestore
        map.put("filterWords", filterWords);

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

    public void updateEvent(String eventId,
                            Map<String, Object> updates,
                            Runnable onSuccess,
                            EventRepository.OnError onError) {
        repo.updateEvent(eventId, updates, onSuccess, onError);
    }

    public void deleteEvent(String eventId,
                            Runnable onSuccess,
                            EventRepository.OnError onError) {
        repo.deleteEvent(eventId, onSuccess, onError);
    }

    private Timestamp toTimestamp(LocalDateTime ldt) {
        if (ldt == null) return null;
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
