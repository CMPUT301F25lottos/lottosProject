package com.example.lottos.events;

import com.example.lottos.EventRepository;
import com.example.lottos.lottery.LotterySystem;
import com.example.lottos.organizer.OrganizerEventDetailsManager;
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
 * Handles all non-UI logic for the EventDetailsScreen.
 * - Loading event + user state
 * - Join/leave waitlist
 * - Accept/decline invite
 * - Run lottery
 */
public class EventDetailsManager {
    private final FirebaseFirestore db;
    private final EventRepository repo;

    public EventDetailsManager() {
        this.db = FirebaseFirestore.getInstance();
        this.repo = new EventRepository(this.db);
    }

    public EventDetailsManager(FirebaseFirestore db, EventRepository repo) {
        this.db = db;
        this.repo = repo;
    }

    public interface LoadCallback {
        void onSuccess(Map<String, Object> eventData, List<String> waitlistUsers, Map<String, Object> userData);
        void onError(Exception e);
    }

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

    public void deleteEvent(String eventName, Runnable onSuccess, Consumer<Exception> onError) {
        repo.getEvent(eventName).delete()
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onError::accept);
    }

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

    public void declineInvite(String eventName, String userName, Runnable onSuccess, EventRepository.OnError onError) {

        DocumentReference eDoc = repo.getEvent(eventName);
        DocumentReference uDoc = db.collection("users").document(userName);

        db.runTransaction(tx -> {
            tx.update(eDoc, "cancelledList.users", FieldValue.arrayUnion(userName));
            tx.update(uDoc, "selectedEvents.events", FieldValue.arrayRemove(eventName));
            tx.update(uDoc, "declinedEvents.events", FieldValue.arrayUnion(eventName));
            return null;

        }).addOnSuccessListener(v -> {
            OrganizerEventDetailsManager organizerManager =
                    new OrganizerEventDetailsManager(db, repo);

            organizerManager.replaceDeclinedUser(
                    eventName,
                    userName,
                    onSuccess,
                    onError::run
            );

        }).addOnFailureListener(e -> onError.run(e));
    }


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
