package com.example.lottos.events;

import com.example.lottos.EventRepository;
import com.example.lottos.lottery.LotterySystem;
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

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final EventRepository repo = new EventRepository();

    // ------------------------------------------------------------------
    // LOAD EVENT + USER
    // ------------------------------------------------------------------
    public interface LoadCallback {
        void onSuccess(Map<String, Object> eventData,
                       List<String> waitlistUsers,
                       Map<String, Object> userData);
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

            // FIX: Declare the list as final. This ensures the variable always points to the same list object.
            final List<String> waitlistUsers = new ArrayList<>();
            Object mapObj = eventSnap.get("waitList");

            if (mapObj instanceof Map) {
                Object usersObj = ((Map<?, ?>) mapObj).get("users");
                if (usersObj instanceof List) {
                    // FIX: Modify the list's contents instead of reassigning the variable.
                    // This is allowed for a final variable.
                    // noinspection unchecked
                    waitlistUsers.addAll((List<String>) usersObj);
                }
            }

            // Now 'waitlistUsers' is effectively final and can be safely used in the lambda below.
            userDoc.get().addOnSuccessListener(userSnap -> {
                Map<String, Object> userData = userSnap.getData();
                cb.onSuccess(eventSnap.getData(), waitlistUsers, userData);
            }).addOnFailureListener(cb::onError);

        }).addOnFailureListener(cb::onError);
    }



    public void deleteEvent(String eventName, Runnable onSuccess, Consumer<Exception> onError) {
        repo.getEvent(eventName)                .delete()
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onError::accept);
    }




    // ------------------------------------------------------------------
    // JOIN WAITLIST
    // ------------------------------------------------------------------
    public void joinWaitlist(String eventName, String userName,
                             Runnable onSuccess,
                             EventRepository.OnError onError) {

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

                }).addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.run(e));
    }

    // ------------------------------------------------------------------
    // LEAVE WAITLIST
    // ------------------------------------------------------------------
    public void leaveWaitlist(String eventName, String userName,
                              Runnable onSuccess,
                              EventRepository.OnError onError) {

        DocumentReference eventDoc = repo.getEvent(eventName);
        DocumentReference userDoc = db.collection("users").document(userName);

        db.runTransaction(tx -> {
                    tx.update(eventDoc, "waitList.users", FieldValue.arrayRemove(userName));
                    tx.update(userDoc, "waitListedEvents.events", FieldValue.arrayRemove(eventName));
                    return null;

                }).addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.run(e));
    }

    // ------------------------------------------------------------------
    // ACCEPT INVITE
    // ------------------------------------------------------------------
    public void acceptInvite(String eventName, String userName,
                             Runnable onSuccess,
                             EventRepository.OnError onError) {

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

    // ------------------------------------------------------------------
    // DECLINE INVITE
    // ------------------------------------------------------------------
    public void declineInvite(String eventName, String userName,
                              Runnable onSuccess,
                              EventRepository.OnError onError) {

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

    public void getEventDetails(String eventName, Consumer<Map<String, Object>> onSuccess, Consumer<Exception> onError) {
        repo.getEvent(eventName).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        onSuccess.accept(documentSnapshot.getData());
                    } else {
                        onSuccess.accept(null); // Event not found
                    }
                })
                .addOnFailureListener(onError::accept);
    }


    // ------------------------------------------------------------------
    // RUN LOTTERY
    // ------------------------------------------------------------------
    public void runLottery(String eventName,
                           List<String> entrants,
                           Runnable onSuccess,
                           EventRepository.OnError onError) {

        DocumentReference eventDoc = repo.getEvent(eventName);

        eventDoc.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                onError.run(new Exception("Event not found"));
                return;
            }

            if (Boolean.TRUE.equals(snap.getBoolean("IsLottery"))) {
                onError.run(new Exception("Lottery already completed"));
                return;
            }

            if (Boolean.TRUE.equals(snap.getBoolean("IsOpen"))) {
                onError.run(new Exception("Event is still open"));
                return;
            }

            if (entrants == null || entrants.isEmpty()) {
                onError.run(new Exception("No entrants in waitlist"));
                return;
            }

            Long selCapLong = snap.getLong("selectionCap");
            int cap = (selCapLong == null) ? entrants.size() : selCapLong.intValue();

            LotterySystem lotto = new LotterySystem(eventName);
            ArrayList<String> selected = lotto.Selected(new ArrayList<>(entrants));

            if (selected.size() > cap) {
                selected = new ArrayList<>(selected.subList(0, cap));
            }

            List<String> finalSelected = new ArrayList<>(selected);
            List<String> notSelected = new ArrayList<>(entrants);
            notSelected.removeAll(finalSelected);

            db.runTransaction(tx -> {
                        // Update event lists
                        tx.update(eventDoc, "selectedList.users", finalSelected);
                        tx.update(eventDoc, "notSelectedList.users", notSelected);

                        // Clear waitlist
                        for (String u : entrants) {
                            tx.update(eventDoc, "waitList.users", FieldValue.arrayRemove(u));
                        }

                        // Mark closed + lottery done
                        tx.update(eventDoc, "IsOpen", false);
                        tx.update(eventDoc, "IsLottery", true);

                        // Update user documents
                        for (String u : finalSelected) {
                            DocumentReference uDoc = db.collection("users").document(u);
                            tx.update(uDoc, "selectedEvents.events", FieldValue.arrayUnion(eventName));
                        }

                        for (String u : notSelected) {
                            DocumentReference uDoc = db.collection("users").document(u);
                            tx.update(uDoc, "notSelectedEvents.events", FieldValue.arrayUnion(eventName));
                        }

                        return null;

                    }).addOnSuccessListener(v -> onSuccess.run())
                    .addOnFailureListener(e -> onError.run(e));

        }).addOnFailureListener(onError::run);
    }
}
