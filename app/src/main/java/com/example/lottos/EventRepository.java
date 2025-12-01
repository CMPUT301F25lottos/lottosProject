package com.example.lottos;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;
import java.util.Map;

/**
 * A repository class for managing CRUD (Create, Read, Update, Delete) operations for events.
 *
 * Role: This class abstracts all direct interactions with the "open events" collection
 * in Firestore. It centralizes the database logic for fetching, creating, updating,
 * and deleting event documents. By providing a clean API for these operations, it
 * decouples the business logic (in managers or ViewModels) from the low-level
 * Firestore implementation details. This makes the code more modular, testable,
 * and easier to maintain.
 */
public class EventRepository {
    private final FirebaseFirestore db;
    private final CollectionReference eventsCollection;

    /**
     * Constructs an EventRepository.
     *
     * @param db The {@link FirebaseFirestore} instance to be used for all database operations.
     */
    public EventRepository(FirebaseFirestore db) {
        this.db = db;
        this.eventsCollection = db.collection("open events");
    }

    /**
     * Gets a direct {@link DocumentReference} to a specific event document.
     *
     * @param eventId The unique identifier of the event.
     * @return A DocumentReference pointing to the event.
     */
    public DocumentReference getEvent(String eventId) {
        return db.collection("open events").document(eventId);
    }

    /**
     * Creates a Firestore {@link Query} to fetch all events created by a specific organizer.
     *
     * @param organizer The username of the event organizer.
     * @return A Query object that, when executed, will return the organizer's events.
     */
    public Query getEventsByOrganizer(String organizer) {
        return db.collection("open events")
                .whereEqualTo("organizer", organizer);
    }

    /**
     * Creates a Firestore {@link Query} to fetch all events in the collection.
     *
     * @return A Query object that, when executed, will return all events.
     */
    public Query getAllEvents() {
        return db.collection("open events");
    }

    /**
     * Creates a new event document in Firestore with the specified data.
     *
     * @param eventId The unique ID to use for the new document.
     * @param data A map containing all the fields and values for the event.
     * @param onSuccess A callback to be executed upon successful creation.
     * @param onError A callback to handle any exceptions that occur.
     */
    public void createEvent(String eventId, Map<String, Object> data, Runnable onSuccess, OnError onError) {

        db.collection("open events")
                .document(eventId)
                .set(data)
                .addOnSuccessListener(x -> onSuccess.run())
                .addOnFailureListener(onError::run);
    }

    /**
     * Updates an existing event document in Firestore with the specified data.
     *
     * @param eventId The unique ID of the document to update.
     * @param data A map containing the fields and new values to be updated.
     * @param onSuccess A callback to be executed upon successful update.
     * @param onError A callback to handle any exceptions that occur.
     */
    public void updateEvent(String eventId, Map<String, Object> data,Runnable onSuccess, OnError onError) {

        db.collection("open events")
                .document(eventId)
                .update(data)
                .addOnSuccessListener(x -> onSuccess.run())
                .addOnFailureListener(onError::run);
    }

    /**
     * Deletes an event document from Firestore.
     *
     * @param eventId The unique ID of the document to delete.
     * @param onSuccess A callback to be executed upon successful deletion.
     * @param onError A callback to handle any exceptions that occur.
     */
    public void deleteEvent(String eventId, Runnable onSuccess, OnError onError) {

        db.collection("open events")
                .document(eventId)
                .delete()
                .addOnSuccessListener(x -> onSuccess.run())
                .addOnFailureListener(onError::run);
    }

    /**
     * A functional interface for handling errors in asynchronous database operations.
     */
    public interface OnError {
        /**
         * The method to be called when an exception occurs.
         * @param e The exception that was caught.
         */
        void run(Exception e);
    }

    /**
     * A functional interface for returning the result of a successful event name fetch.
     */
    public interface OnNameResult {
        /**
         * The method to be called with the resulting event name.
         * @param eventName The fetched name of the event.
         */
        void run(String eventName);
    }

    /**
     * Asynchronously fetches the name of an event given its ID.
     * If the event name is not found or is empty, it returns the eventId as a fallback.
     *
     * @param eventId The ID of the event to look up.
     * @param onSuccess The callback to run with the resulting event name.
     * @param onError The callback to run if a database error occurs.
     */
    public void getEventName(String eventId, OnNameResult onSuccess, OnError onError) {

        db.collection("open events")
                .document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String name = snapshot.getString("eventName");
                        if (name != null && !name.isEmpty()) {
                            onSuccess.run(name);
                            return;
                        }
                    }
                    onSuccess.run(eventId); // fallback
                })
                .addOnFailureListener(onError::run);
    }
}
