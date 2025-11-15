package com.example.lottos.events;

import com.example.lottos.EventRepository;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles all business logic for entrant events.
 * - Loading events for UI
 * - Join/leave waitlist
 * No UI code here.
 */
public class EntrantEventManager {

    private final EventRepository repo = new EventRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Data model for UI */
    public static class EventModel {
        public String id;
        public String name;
        public boolean isOpen;

        public EventModel(String id, String name, boolean isOpen) {
            this.id = id;
            this.name = name;
            this.isOpen = isOpen;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Callback interface for loading events */
    public interface EventsCallback {
        void onSuccess(List<EventModel> events, List<String> userWaitlistedEvents);
        void onError(Exception e);
    }

    /** Load all open events + user's waitlisted events */
    /** Load all open events + user's waitlisted events */
    public void loadEventsForUser(String userName, EventsCallback callback) {
        // Step 1: load user waitlisted events
        db.collection("users").document(userName).get()
                .addOnSuccessListener(userSnap -> {
                    // Make the list final and add items to it instead of reassigning.
                    final List<String> waitlisted = new ArrayList<>();
                    Object data = userSnap.get("waitListedEvents.events");
                    if (data instanceof List) {
                        // This is safe because we are modifying the list's contents,
                        // not reassigning the 'waitlisted' variable itself.
                        waitlisted.addAll((List<String>) data);
                    }

                    // Step 2: load all events
                    repo.getAllEvents().get()
                            .addOnSuccessListener(query -> {
                                List<EventModel> result = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : query) {
                                    String id = doc.getId();
                                    String name = doc.getString("eventName");
                                    Boolean openFlag = doc.getBoolean("IsOpen");

                                    if (name != null && openFlag != null && openFlag) {
                                        result.add(new EventModel(id, name, true));
                                    }
                                }
                                // Now 'waitlisted' is effectively final and can be used here.
                                callback.onSuccess(result, waitlisted);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }


    /** Join an event waitlist */
    public void joinWaitlist(String userName, String eventId, Runnable onSuccess, EventRepository.OnError onError) {
        DocumentReference eventRef = repo.getEvent(eventId);
        DocumentReference userRef = db.collection("users").document(userName);

        eventRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                onError.run(new Exception("Event not found"));
                return;
            }

            Map<String, Object> waitMap = (Map<String, Object>) snap.get("waitList");
            List<String> waitUsers = new ArrayList<>();
            if (waitMap != null && waitMap.get("users") instanceof List) {
                waitUsers = (List<String>) waitMap.get("users");
            }

            if (waitUsers.contains(userName)) {
                onError.run(new Exception("Already on waitlist"));
                return;
            }

            Long capLong = snap.getLong("waitListCapacity");
            int capacity = capLong != null ? capLong.intValue() : 0;
            if (capacity > 0 && waitUsers.size() >= capacity) {
                onError.run(new Exception("Waitlist full"));
                return;
            }

            waitUsers.add(userName);

            eventRef.update("waitList.users", waitUsers)
                    .addOnSuccessListener(v ->
                            userRef.update("waitListedEvents.events", FieldValue.arrayUnion(eventId))
                                    .addOnSuccessListener(x -> onSuccess.run())
                                    .addOnFailureListener(onError::run))
                    .addOnFailureListener(onError::run);

        }).addOnFailureListener(onError::run);
    }

    /** Leave an event waitlist */
    public void leaveWaitlist(String userName, String eventId, Runnable onSuccess, EventRepository.OnError onError) {
        DocumentReference eventRef = repo.getEvent(eventId);
        DocumentReference userRef = db.collection("users").document(userName);

        eventRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                onError.run(new Exception("Event not found"));
                return;
            }

            Map<String, Object> waitMap = (Map<String, Object>) snap.get("waitList");
            List<String> waitUsers = new ArrayList<>();
            if (waitMap != null && waitMap.get("users") instanceof List) {
                waitUsers = (List<String>) waitMap.get("users");
            }

            if (!waitUsers.contains(userName)) {
                onError.run(new Exception("Not on waitlist"));
                return;
            }

            waitUsers.remove(userName);

            eventRef.update("waitList.users", waitUsers)
                    .addOnSuccessListener(v ->
                            userRef.update("waitListedEvents.events", FieldValue.arrayRemove(eventId))
                                    .addOnSuccessListener(x -> onSuccess.run())
                                    .addOnFailureListener(onError::run))
                    .addOnFailureListener(onError::run);

        }).addOnFailureListener(onError::run);
    }
}
