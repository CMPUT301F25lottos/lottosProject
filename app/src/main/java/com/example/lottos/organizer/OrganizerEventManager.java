package com.example.lottos.organizer;

import com.example.lottos.EventRepository;
import com.example.lottos.entities.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class OrganizerEventManager {

    private final EventRepository repo;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public OrganizerEventManager() {
        // If your EventRepository needs a db, wire it here
        FirebaseFirestore dbInstance = FirebaseFirestore.getInstance();
        this.db = dbInstance;
        this.repo = new EventRepository(dbInstance); // or new EventRepository() if that exists
        this.auth = FirebaseAuth.getInstance();
    }

    public interface LocationsCallback {
        void onSuccess(Map<String, Map<String, Double>> locations);
        void onError(Exception e);
    }

    public void getEntrantLocations(String eventId, LocationsCallback callback) {
        db.collection("events").document(eventId).collection("geoLocations")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Map<String, Double>> locationData = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String userName = doc.getId(); // Document ID is the username

                        Double lat = doc.getDouble("latitude");
                        Double lon = doc.getDouble("longitude");

                        if (lat != null && lon != null) {
                            Map<String, Double> coords = new HashMap<>();
                            coords.put("latitude", lat);
                            coords.put("longitude", lon);
                            locationData.put(userName, coords);
                        }
                    }
                    callback.onSuccess(locationData);
                })
                .addOnFailureListener(callback::onError);
    }

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
            boolean geolocationRequired,
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

        map.put("geolocationRequired", geolocationRequired);

        map.put("posterUrl", event.getPosterUrl());

        if (waitListCapacity != null) {
            map.put("waitListCapacity", waitListCapacity);
        }

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

    public void saveEntrantLocation(String eventId,
                                    String userId,
                                    double latitude,
                                    double longitude,
                                    Runnable onSuccess,
                                    EventRepository.OnError onError) {

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("userId", userId);
        locationData.put("joinTimestamp", Timestamp.now());
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);

        db.collection("open events")
                .document(eventId)
                // New sub-collection to store location data
                .collection("geo_locations")
                .document(userId)
                .set(locationData)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(onError::run);
    }
}
