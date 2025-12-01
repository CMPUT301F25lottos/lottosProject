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

/**
 * Manages the business logic for event operations performed by an organizer.
 *
 * Role: This class acts as a bridge between the organizer-facing UI (like
 * {@link CreateEventScreen} or {@link EditEventScreen}) and the underlying data sources
 * (Firestore and {@link EventRepository}). Its key responsibilities include:
 * <ul>
 *     <li>Constructing the full event document data map for creating new events.</li>
 *     <li>Delegating create, update, and delete operations to the EventRepository.</li>
 *     <li>Handling organizer-specific logic, such as updating an organizer's personal list of managed events.</li>
 *     <li>Fetching and saving event-specific geolocation data for attendees.</li>
 *     <li>Providing helper methods for data conversion (e.g., LocalDateTime to Timestamp).</li>
 * </ul>
 */
public class OrganizerEventManager {

    private final EventRepository repo;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    /**
     * Default no-argument constructor used by Fragments for easy instantiation.
     * It initializes its own instances of Firestore, the EventRepository, and FirebaseAuth.
     */
    public OrganizerEventManager() {
        FirebaseFirestore dbInstance = FirebaseFirestore.getInstance();
        this.db = dbInstance;
        this.repo = new EventRepository(dbInstance);
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * A callback interface for asynchronous fetching of location data.
     */
    public interface LocationsCallback {
        /**
         * Called on successful retrieval of location data.
         * @param locations A map where the key is the username and the value is another map
         *                  containing "latitude" and "longitude" keys.
         */
        void onSuccess(Map<String, Map<String, Double>> locations);
        /**
         * Called when an error occurs during the data fetching.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    // -------------------------------------------------------------
    // 🔥 FIXED: LOAD GEOLOCATIONS USING EVENT NAME AS THE DOCUMENT ID
    // -------------------------------------------------------------
    public void getEntrantLocations(String eventName, LocationsCallback callback) {

        db.collection("open events")
                .document(eventName)                // ✔ eventName instead of eventId
                .collection("geo_locations")
    /**
     * Retrieves the stored geographic locations of all entrants for a specific event.
     * It reads from the `geo_locations` sub-collection of an event document.
     *
     * @param eventId The unique ID of the event for which to fetch locations.
     * @param callback The callback to handle the successfully retrieved map of locations or an error.
     */
    public void getEntrantLocations(String eventId, LocationsCallback callback) {
        db.collection("open events").document(eventId).collection("geo_locations")
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

    /**
     * Full constructor for dependency injection, primarily used for testing.
     * Allows providing mock or custom instances of its dependencies.
     * @param repo The EventRepository to use for data operations.
     * @param db The FirebaseFirestore instance to use.
     * @param auth The FirebaseAuth instance to use.
     */
    public OrganizerEventManager(EventRepository repo,
                                 FirebaseFirestore db,
                                 FirebaseAuth auth) {
        this.repo = repo;
        this.db = db;
        this.auth = auth;
    }

    /**
     * Creates a new event in Firestore.
     * This method builds the complete data map for the event document, including initializing
     * all user lists, and then calls the repository to perform the creation. Upon success,
     * it also updates the organizer's user document to add this new event to their organized list.
     *
     * @param event The Event entity containing the core details.
     * @param registerEndTime The specific date and time when registration closes.
     * @param waitListCapacity The maximum number of users allowed on the waitlist (can be null).
     * @param filterWords A list of keywords for event filtering.
     * @param geolocationRequired A boolean indicating if attendees must provide their location to check in.
     * @param onSuccess A callback to be executed upon successful creation of the event.
     * @param onError A callback to handle any errors that occur during the process.
     */
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

    /**
     * Updates an existing event document in Firestore.
     * Delegates the call directly to the EventRepository.
     *
     * @param eventId The unique ID of the event to update.
     * @param updates A map of fields and their new values to be updated.
     * @param onSuccess A callback to run on successful update.
     * @param onError A callback to handle any errors.
     */
    public void updateEvent(String eventId,
                            Map<String, Object> updates,
                            Runnable onSuccess,
                            EventRepository.OnError onError) {
        repo.updateEvent(eventId, updates, onSuccess, onError);
    }

    /**
     * Deletes an event document from Firestore.
     * Delegates the call directly to the EventRepository.
     *
     * @param eventId The unique ID of the event to delete.
     * @param onSuccess A callback to run on successful deletion.
     * @param onError A callback to handle any errors.
     */
    public void deleteEvent(String eventId,
                            Runnable onSuccess,
                            EventRepository.OnError onError) {
        repo.deleteEvent(eventId, onSuccess, onError);
    }

    /**
     * Converts a {@link LocalDateTime} object to a Firebase {@link Timestamp}.
     * Returns null if the input is null.
     * @param ldt The LocalDateTime to convert.
     * @return The corresponding Firebase Timestamp, or null.
     */
    private Timestamp toTimestamp(LocalDateTime ldt) {
        if (ldt == null) return null;
        java.util.Date date =
                java.util.Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        return new Timestamp(date);
    }

    /**
     * Creates a default map structure for an event's waitlist.
     * @return A map with predefined keys and an empty user list.
     */
    private Map<String, Object> makeWaitListMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("closeDate", "");
        m.put("closeTime", "");
        m.put("users", new ArrayList<String>());
        return m;
    }

    /**
     * Creates a default map structure for a generic user list (e.g., enrolled, selected).
     * @return A map with a "users" key and an empty list.
     */
    private Map<String, Object> makeUserListMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("users", new ArrayList<String>());
        return m;
    }

    /**
     * Saves an entrant's geographic location to a sub-collection within the event document.
     * This is typically used for check-in purposes.
     *
     * @param eventId The ID of the event the user is checking into.
     * @param userId The username of the entrant.
     * @param latitude The entrant's current latitude.
     * @param longitude The entrant's current longitude.
     * @param onSuccess A callback to run on success.
     * @param onError A callback to handle failures.
     */
    public void saveEntrantLocation(String eventId,
                                    String userId,
                                    double latitude,
                                    double longitude,
                                    Runnable onSuccess,
                                    EventRepository.OnError onError) {

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
