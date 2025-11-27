package com.example.lottos.organizer;

import com.example.lottos.EventRepository;
import com.example.lottos.lottery.LotterySystem;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Non-UI logic for OrganizerEventDetailsScreen.
 * Loads event metadata + all participant lists for an eventId.
 *
 * Lists (from Firestore):
 * - waitList.users
 * - selectedList.users
 * - notSelectedList.users
 * - enrolledList.users
 * - cancelledList.users
 */
public class OrganizerEventDetailsManager {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final EventRepository repo = new EventRepository();

    public interface LoadCallback {
        void onSuccess(Map<String, Object> eventData,
                       List<String> waitlistUsers,
                       List<String> selectedUsers,
                       List<String> notSelectedUsers,
                       List<String> enrolledUsers,
                       List<String> cancelledUsers);

        void onError(Exception e);
    }

    /**
     * Loads a single event by document ID (eventId).
     */
    public void loadEvent(String eventId, LoadCallback cb) {
        DocumentReference eventDoc = repo.getEvent(eventId);

        eventDoc.get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        cb.onError(new Exception("Event not found"));
                        return;
                    }

                    Map<String, Object> data = snap.getData();

                    List<String> waitlistUsers   = extractUserList(data, "waitList");
                    List<String> selectedUsers   = extractUserList(data, "selectedList");
                    List<String> notSelectedUsers= extractUserList(data, "notSelectedList");
                    List<String> enrolledUsers   = extractUserList(data, "enrolledList");
                    List<String> cancelledUsers  = extractUserList(data, "cancelledList");

                    cb.onSuccess(data,
                            waitlistUsers,
                            selectedUsers,
                            notSelectedUsers,
                            enrolledUsers,
                            cancelledUsers);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Helper for Firestore structure of the form:
     *   "<key>": { "users": [ "u1", "u2", ... ] }
     */
    @SuppressWarnings("unchecked")
    private List<String> extractUserList(Map<String, Object> eventData, String key) {
        List<String> result = new ArrayList<>();
        if (eventData == null) return result;

        Object parent = eventData.get(key);
        if (!(parent instanceof Map)) return result;

        Object listObj = ((Map<?, ?>) parent).get("users");
        if (listObj instanceof List) {
            for (Object u : (List<?>) listObj) {
                if (u != null) {
                    result.add(u.toString());
                }
            }
        }
        return result;
    }

    public void runLottery(String eventId,
                           List<String> waitUsers,
                           Runnable onSuccess,
                           java.util.function.Consumer<Exception> onError) {

        // Safety check: nothing to do if nobody is on the waitlist
        if (waitUsers == null || waitUsers.isEmpty()) {
            onError.accept(new Exception("No users on waitlist to run lottery."));
            return;
        }

        DocumentReference eventRef = repo.getEvent(eventId); // points to "open events/{eventId}"

        eventRef.get()
                .addOnSuccessListener(eventSnap -> {
                    if (!eventSnap.exists()) {
                        onError.accept(new Exception("Event not found"));
                        return;
                    }

                    String eventName   = eventSnap.getString("eventName");
                    String organizer   = eventSnap.getString("organizer");
                    Long selectionCapL = eventSnap.getLong("selectionCap");
                    int selectionCap   = (selectionCapL != null && selectionCapL > 0)
                            ? selectionCapL.intValue()
                            : waitUsers.size(); // fallback: everyone can be selected

                    // 1. Shuffle and split waitlist into selected + not selected
                    List<String> shuffled = new ArrayList<>(waitUsers);
                    java.util.Collections.shuffle(shuffled);

                    List<String> selectedUsers    = new ArrayList<>();
                    List<String> notSelectedUsers = new ArrayList<>();

                    for (int i = 0; i < shuffled.size(); i++) {
                        if (i < selectionCap) {
                            selectedUsers.add(shuffled.get(i));
                        } else {
                            notSelectedUsers.add(shuffled.get(i));
                        }
                    }

                    // 2. Start a batch write
                    WriteBatch batch = db.batch();

                    // 2a) Update event doc lists in "open events/{eventId}"
                    Map<String, Object> eventUpdates = new HashMap<>();
                    eventUpdates.put("IsLottery", true);

                    Map<String, Object> selectedListMap = new HashMap<>();
                    selectedListMap.put("users", selectedUsers);
                    eventUpdates.put("selectedList", selectedListMap);

                    Map<String, Object> notSelectedListMap = new HashMap<>();
                    notSelectedListMap.put("users", notSelectedUsers);
                    eventUpdates.put("notSelectedList", notSelectedListMap);

                    // Clear waitList.users
                    Map<String, Object> waitListMap = new HashMap<>();
                    waitListMap.put("users", new ArrayList<String>());
                    eventUpdates.put("waitList", waitListMap);

                    batch.update(eventRef, eventUpdates);

                    // 2b) Update each user doc:
                    // move this event from waitListedEvents.events → selectedEvents / notSelectedEvents
                    String eventKeyForUser = eventId; // we store event *ID* in user docs

                    for (String userId : selectedUsers) {
                        DocumentReference userRef = db.collection("users").document(userId);
                        batch.update(userRef,
                                "waitListedEvents.events", FieldValue.arrayRemove(eventKeyForUser),
                                "selectedEvents.events",   FieldValue.arrayUnion(eventKeyForUser)
                        );
                    }

                    for (String userId : notSelectedUsers) {
                        DocumentReference userRef = db.collection("users").document(userId);
                        batch.update(userRef,
                                "waitListedEvents.events",  FieldValue.arrayRemove(eventKeyForUser),
                                "notSelectedEvents.events", FieldValue.arrayUnion(eventKeyForUser)
                        );
                    }

                    // 3) Create notifications in the same batch
                    sendLotteryNotifications(
                            batch,
                            eventName != null ? eventName : eventId,
                            organizer,
                            selectedUsers,
                            notSelectedUsers
                    );

                    // 4) Commit the batch
                    batch.commit()
                            .addOnSuccessListener(v -> onSuccess.run())
                            .addOnFailureListener(onError::accept);

                })
                .addOnFailureListener(onError::accept);
    }


    private void sendLotteryNotifications(WriteBatch batch,
                                          String eventName,
                                          String organizer,
                                          List<String> selectedUsers,
                                          List<String> notSelectedUsers) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

        // Selected users
        for (String user : selectedUsers) {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("content", "You have been SELECTED for " + eventName + "go to event detail page to accept then invite");
            notifData.put("eventName", eventName);
            notifData.put("receiver", user);
            notifData.put("sender", organizer != null ? organizer : "System");
            notifData.put("timestamp", now);

            DocumentReference notifRef = db.collection("notification").document();
            batch.set(notifRef, notifData);
        }

        // Not selected users
        for (String user : notSelectedUsers) {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("content", "You were NOT selected for " + eventName);
            notifData.put("eventName", eventName);
            notifData.put("receiver", user);
            notifData.put("sender", organizer != null ? organizer : "System");
            notifData.put("timestamp", now);

            DocumentReference notifRef = db.collection("notification").document();
            batch.set(notifRef, notifData);
        }
    }


}
