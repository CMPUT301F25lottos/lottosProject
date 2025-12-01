package com.example.lottos.events;

import com.example.lottos.EventRepository;
import com.example.lottos.lottery.LotterySystem;
import com.example.lottos.organizer.OrganizerEventManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Handles the business logic for the event details screen, acting as a bridge between the UI and the data layer.
 *
 * Role: This class is responsible for all non-UI logic related to viewing and interacting
 * with a single event. Its responsibilities include:
 * <ul>
 *     <li>Loading detailed information about a specific event and the current user's status relative to it.</li>
 *     <li>Handling user actions such as joining or leaving a waitlist.</li>
 *     <li>Managing an entrant's response to an invitation (accept/decline).</li>
 *     <li>Providing functionality for an organizer to delete an event.</li>
 * </ul>
 * It orchestrates database operations through Firestore transactions to ensure data consistency.
 */
public class EventDetailsManager {
    private final FirebaseFirestore db;
    private final EventRepository repo;

    /**
     * Default constructor that initializes its own FirebaseFirestore and EventRepository instances.
     */
    public EventDetailsManager() {
        this.db = FirebaseFirestore.getInstance();
        this.repo = new EventRepository(this.db);
    }

    /**
     * Constructor for dependency injection, allowing for custom FirebaseFirestore and EventRepository instances.
     * Useful for testing with mock objects.
     *
     * @param db The FirebaseFirestore instance to use.
     * @param repo The EventRepository to use for data access.
     */
    public EventDetailsManager(FirebaseFirestore db, EventRepository repo) {
        this.db = db;
        this.repo = repo;
    }

    /**
     * A callback interface for asynchronous loading of event and user data.
     */
    public interface LoadCallback {
        /**
         * Called on a successful data load.
         *
         * @param eventData A map containing the event's properties.
         * @param waitlistUsers A list of usernames currently on the event's waitlist.
         * @param userData A map containing the current user's properties.
         */
        void onSuccess(Map<String, Object> eventData, List<String> waitlistUsers, Map<String, Object> userData);
        /**
         * Called when a data loading error occurs.
         *
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Loads all necessary data for an entrant viewing an event's details page.
     * It fetches the event document, its waitlist, and the user's document in parallel.
     *
     * @param eventName The name of the event to load.
     * @param userName The username of the entrant viewing the page.
     * @param cb The callback to be invoked with the results or an error.
     */
    public void loadEventForEntrant(String eventName, String userName, LoadCallback cb) {
        DocumentReference eventDoc = repo.getEvent(eventName);
        DocumentReference userDoc = db.collection("users").document(userName);

        eventDoc.get().addOnSuccessListener(eventSnap -> {
            if (!eventSnap.exists()) {
                cb.onError(new Exception("Event not found"));
                return;
            }

            Map<String, Object> eventData = eventSnap.getData();
            if (eventData == null) {
                cb.onError(new Exception("Event data is null"));
                return;
            }

            final List<String> waitlistUsers = new ArrayList<>();
            Object mapObj = eventSnap.get("waitList");

            if (mapObj instanceof Map) {
                Object usersObj = ((Map<?, ?>) mapObj).get("users");
                if (usersObj instanceof List) {
                    waitlistUsers.addAll((List<String>) usersObj);
                }
            }

            userDoc.get().addOnSuccessListener(userSnap -> {
                Map<String, Object> userData = userSnap.getData();
                cb.onSuccess(eventData, waitlistUsers, userData);
            }).addOnFailureListener(cb::onError);

        }).addOnFailureListener(cb::onError);
    }

    /**
     * Deletes an event document from Firestore.
     * This is an organizer-only action.
     *
     * @param eventName The name of the event to delete.
     * @param onSuccess A Runnable to be executed on successful deletion.
     * @param onError A Consumer to handle any exceptions during deletion.
     */
    public void deleteEvent(String eventName, Runnable onSuccess, Consumer<Exception> onError) {
        repo.getEvent(eventName).delete()
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onError::accept);
    }

    /**
     * Allows a user to join the waitlist for a specific event.
     * This operation is performed in a transaction to ensure the event is still open and to
     * atomically update both the event's waitlist and the user's list of waitlisted events.
     * Also saves the user's geolocation if required by the event.
     *
     * @param eventName The name of the event to join.
     * @param userName The username of the entrant joining.
     * @param latitude The user's current latitude.
     * @param longitude The user's current longitude.
     * @param onSuccess A Runnable to be executed on success.
     * @param onError A callback to handle any exceptions.
     */
    public void joinWaitlist(String eventName, String userName, double latitude, double longitude, Runnable onSuccess, EventRepository.OnError onError) {
        DocumentReference eventDoc = repo.getEvent(eventName);
        DocumentReference userDoc = db.collection("users").document(userName);

        db.runTransaction(tx -> {
                    Boolean open = tx.get(eventDoc).getBoolean("IsOpen");
                    if (!Boolean.TRUE.equals(open)) {
                        throw new FirebaseFirestoreException("This event is closed.",
                                FirebaseFirestoreException.Code.ABORTED);
                    }

                    tx.update(eventDoc, "waitList.users", FieldValue.arrayUnion(userName));
                    tx.update(userDoc, "waitListedEvents.events", FieldValue.arrayUnion(eventName));
                    return null;

                }).addOnSuccessListener(v -> {
                    OrganizerEventManager organizerManager = new OrganizerEventManager();

                    organizerManager.saveEntrantLocation(eventName, userName, latitude, longitude,
                            () -> onSuccess.run(),
                            e -> {
                                System.err.println("Warning: Failed to save geo location: " + e.getMessage());
                                onSuccess.run();
                            });

                })
                .addOnFailureListener(e -> onError.run(e));
    }

    /**
     * Allows a user to leave the waitlist for a specific event.
     * This operation is performed in a transaction to atomically update both the event's
     * waitlist and the user's list of waitlisted events.
     *
     * @param eventName The name of the event to leave.
     * @param userName The username of the entrant leaving.
     * @param onSuccess A Runnable to be executed on success.
     * @param onError A callback to handle any exceptions.
     */
    public void leaveWaitlist(String eventName, String userName, Runnable onSuccess, EventRepository.OnError onError) {

        DocumentReference eventDoc = repo.getEvent(eventName);
        DocumentReference userDoc = db.collection("users").document(userName);

        db.runTransaction(tx -> {
                    tx.update(eventDoc, "waitList.users", FieldValue.arrayRemove(userName));
                    tx.update(userDoc, "waitListedEvents.events", FieldValue.arrayRemove(eventName));
                    return null;

                }).addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.run(e));
    }

    /**
     * Allows a user to accept an invitation to an event.
     * This transactionally moves the user from the event's selected list to the enrolled list
     * and updates the user's corresponding event lists.
     *
     * @param eventName The name of the event.
     * @param userName The username of the entrant accepting.
     * @param onSuccess A Runnable to be executed on success.
     * @param onError A callback to handle any exceptions.
     */
    public void acceptInvite(String eventName, String userName, Runnable onSuccess, EventRepository.OnError onError) {

        DocumentReference eDoc = repo.getEvent(eventName);
        DocumentReference uDoc = db.collection("users").document(userName);

        db.runTransaction(tx -> {
                    tx.update(eDoc, "enrolledList.users", FieldValue.arrayUnion(userName));
                    tx.update(uDoc, "selectedEvents.events", FieldValue.arrayRemove(eventName));
                    tx.update(uDoc, "enrolledEvents.events", FieldValue.arrayUnion(eventName));
                    return null;

                }).addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.run(e));
    }

    /**
     * Allows a user to decline an invitation to an event.
     * This transactionally moves the user from the event's selected list to the cancelled list
     * and updates the user's corresponding event lists.
     *
     * @param eventName The name of the event.
     * @param userName The username of the entrant declining.
     * @param onSuccess A Runnable to be executed on success.
     * @param onError A callback to handle any exceptions.
     */
    public void declineInvite(String eventName, String userName, Runnable onSuccess, EventRepository.OnError onError) {

        DocumentReference eDoc = repo.getEvent(eventName);
        DocumentReference uDoc = db.collection("users").document(userName);

        db.runTransaction(tx -> {
                    tx.update(eDoc, "cancelledList.users", FieldValue.arrayUnion(userName));
                    tx.update(uDoc, "selectedEvents.events", FieldValue.arrayRemove(eventName));
                    tx.update(uDoc, "declinedEvents.events", FieldValue.arrayUnion(eventName));
                    return null;

                }).addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.run(e));
    }

    /**
     * Fetches the raw data for a single event document.
     *
     * @param eventName The name of the event to fetch.
     * @param onSuccess A Consumer that accepts a map of the event data if found.
     * @param onError A Consumer that handles any exceptions.
     */
    public void getEventDetails(String eventName, Consumer<Map<String, Object>> onSuccess, Consumer<Exception> onError) {
        repo.getEvent(eventName).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        onSuccess.accept(documentSnapshot.getData());
                    } else {
                        onSuccess.accept(null);
                    }
                })
                .addOnFailureListener(onError::accept);
    }

}
