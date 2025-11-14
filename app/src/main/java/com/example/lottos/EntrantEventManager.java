package com.example.lottos;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

/**
 * Handles join/leave waitlist logic for entrants.
 * No UI code in this class.
 */
public class EntrantEventManager {

    private final EventRepository repo = new EventRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void joinWaitlist(
            String userName,
            String eventId,
            Runnable onSuccess,
            EventRepository.OnError onError
    ) {
        DocumentReference eventRef = repo.getEvent(eventId);
        DocumentReference userRef = db.collection("users").document(userName);

        eventRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                onError.run(new Exception("Event not found"));
                return;
            }

            Map<String, Object> waitMap = (Map<String, Object>) snap.get("waitList");
            List<String> waitUsers;

            if (waitMap != null && waitMap.get("users") instanceof List) {
                waitUsers = (List<String>) waitMap.get("users");
            } else {
                waitUsers = new ArrayList<>();
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
                    .addOnSuccessListener(v -> {
                        userRef.update("waitListedEvents.events",
                                        FieldValue.arrayUnion(eventId))
                                .addOnSuccessListener(x -> onSuccess.run())
                                .addOnFailureListener(onError::run);
                    })
                    .addOnFailureListener(onError::run);
        }).addOnFailureListener(onError::run);
    }

    public void leaveWaitlist(
            String userName,
            String eventId,
            Runnable onSuccess,
            EventRepository.OnError onError
    ) {
        DocumentReference eventRef = repo.getEvent(eventId);
        DocumentReference userRef = db.collection("users").document(userName);

        eventRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                onError.run(new Exception("Event not found"));
                return;
            }

            Map<String, Object> waitMap = (Map<String, Object>) snap.get("waitList");
            List<String> waitUsers;

            if (waitMap != null && waitMap.get("users") instanceof List) {
                waitUsers = (List<String>) waitMap.get("users");
            } else {
                waitUsers = new ArrayList<>();
            }

            if (!waitUsers.contains(userName)) {
                onError.run(new Exception("Not on waitlist"));
                return;
            }

            waitUsers.remove(userName);

            eventRef.update("waitList.users", waitUsers)
                    .addOnSuccessListener(v -> {
                        userRef.update("waitListedEvents.events",
                                        FieldValue.arrayRemove(eventId))
                                .addOnSuccessListener(x -> onSuccess.run())
                                .addOnFailureListener(onError::run);
                    })
                    .addOnFailureListener(onError::run);
        }).addOnFailureListener(onError::run);
    }
}
