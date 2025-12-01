package com.example.lottos;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;
import java.util.Map;

public class EventRepository {
    private final FirebaseFirestore db;
    private final CollectionReference eventsCollection;

    public EventRepository(FirebaseFirestore db) {
        this.db = db;
        this.eventsCollection = db.collection("open events");
    }

    public DocumentReference getEvent(String eventId) {
        return db.collection("open events").document(eventId);
    }
    public Query getEventsByOrganizer(String organizer) {
        return db.collection("open events")
                .whereEqualTo("organizer", organizer);
    }
    public Query getAllEvents() {
        return db.collection("open events");
    }
    public void createEvent(String eventId, Map<String, Object> data, Runnable onSuccess, OnError onError) {

        db.collection("open events")
                .document(eventId)
                .set(data)
                .addOnSuccessListener(x -> onSuccess.run())
                .addOnFailureListener(onError::run);
    }

    public void updateEvent(String eventId, Map<String, Object> data,Runnable onSuccess, OnError onError) {

        db.collection("open events")
                .document(eventId)
                .update(data)
                .addOnSuccessListener(x -> onSuccess.run())
                .addOnFailureListener(onError::run);
    }

    public void deleteEvent(String eventId, Runnable onSuccess, OnError onError) {

        db.collection("open events")
                .document(eventId)
                .delete()
                .addOnSuccessListener(x -> onSuccess.run())
                .addOnFailureListener(onError::run);
    }

    public interface OnError {
        void run(Exception e);
    }

    public interface OnNameResult {
        void run(String eventName);
    }

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
