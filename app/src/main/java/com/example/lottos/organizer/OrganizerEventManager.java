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

public class OrganizerEventManager {

    private final EventRepository repo;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    /** DEFAULT CONSTRUCTOR USED BY UI */
    public OrganizerEventManager() {
        FirebaseFirestore dbInstance = FirebaseFirestore.getInstance();
        this.db = dbInstance;
        this.repo = new EventRepository(dbInstance);
        this.auth = FirebaseAuth.getInstance();
    }

    /** CALLBACK FOR MAP LOCATION LOADING */
    public interface LocationsCallback {
        void onSuccess(Map<String, Map<String, Double>> locations);
        void onError(Exception e);
    }

    // -------------------------------------------------------------
    // 🔥 FIXED: LOAD GEOLOCATIONS USING EVENT NAME AS THE DOCUMENT ID
    // -------------------------------------------------------------
    public void getEntrantLocations(String eventName, LocationsCallback callback) {

        db.collection("open events")
                .document(eventName)                // ✔ eventName instead of eventId
                .collection("geo_locations")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    Map<String, Map<String, Double>> results = new HashMap<>();

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        callback.onSuccess(results);
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot) {
                        String userId = doc.getId();
                        Double lat = doc.getDouble("latitude");
                        Double lon = doc.getDouble("longitude");

                        if (lat == null || lon == null) continue;
                        if (lat == 0.0 && lon == 0.0) continue;

                        Map<String, Double> coords = new HashMap<>();
                        coords.put("latitude", lat);
                        coords.put("longitude", lon);
                        results.put(userId, coords);
                    }

                    callback.onSuccess(results);
                })
                .addOnFailureListener(callback::onError);
    }

    // -------------------------------------------------------------
    // CORE EVENT MANAGEMENT METHODS (UNCHANGED)
    // -------------------------------------------------------------

    public OrganizerEventManager(EventRepository repo, FirebaseFirestore db, FirebaseAuth auth) {
        this.repo = repo;
        this.db = db;
        this.auth = auth;
    }

    public void createEvent(
            Event event,
            LocalDateTime registerEndTime,
            Integer waitListCapacity,
            List<String> filterWords,
            boolean geolocationRequired,
            Runnable onSuccess,
            EventRepository.OnError onError
    ) {

        String eventId = event.getEventName();
        // ✔ You ALREADY use eventName as the Firestore document key.

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

    // -------------------------------------------------------------
    // 🔥 FIXED: SAVE LOCATION USING EVENT NAME, NOT EVENT ID
    // -------------------------------------------------------------
    public void saveEntrantLocation(
            String eventName,
            String userId,
            double latitude,
            double longitude,
            Runnable onSuccess,
            EventRepository.OnError onError
    ) {

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("userId", userId);
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("timestamp", Timestamp.now());

        db.collection("open events")
                .document(eventName)                // ✔ matches getEntrantLocations()
                .collection("geo_locations")
                .document(userId)
                .set(locationData)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(onError::run);
    }

}
