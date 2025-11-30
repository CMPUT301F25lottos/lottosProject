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

public class OrganizerEventDetailsManager {

    private final FirebaseFirestore db;
    private final EventRepository repo;

    public OrganizerEventDetailsManager(FirebaseFirestore db, EventRepository repo) {
        this.db = db;
        this.repo = repo;
    }

    public OrganizerEventDetailsManager() {
        this.db = FirebaseFirestore.getInstance();
        this.repo = new EventRepository(this.db);
    }

    public interface LoadCallback {
        void onSuccess(Map<String, Object> eventData, List<String> waitlistUsers, List<String> selectedUsers, List<String> notSelectedUsers, List<String> enrolledUsers, List<String> cancelledUsers);
        void onError(Exception e);
    }

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

                    cb.onSuccess(data, waitlistUsers, selectedUsers, notSelectedUsers, enrolledUsers, cancelledUsers);
                })
                .addOnFailureListener(cb::onError);
    }

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

    public void runLottery(String eventId, List<String> waitUsers, Runnable onSuccess, java.util.function.Consumer<Exception> onError) {

        if (waitUsers == null || waitUsers.isEmpty()) {
            onError.accept(new Exception("No users on waitlist to run lottery."));
            return;
        }

        DocumentReference eventRef = repo.getEvent(eventId);

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
                            : waitUsers.size();

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

                    WriteBatch batch = db.batch();

                    Map<String, Object> eventUpdates = new HashMap<>();
                    eventUpdates.put("IsLottery", true);

                    Map<String, Object> selectedListMap = new HashMap<>();
                    selectedListMap.put("users", selectedUsers);
                    eventUpdates.put("selectedList", selectedListMap);

                    Map<String, Object> notSelectedListMap = new HashMap<>();
                    notSelectedListMap.put("users", notSelectedUsers);
                    eventUpdates.put("notSelectedList", notSelectedListMap);

                    Map<String, Object> waitListMap = new HashMap<>();
                    waitListMap.put("users", new ArrayList<String>());
                    eventUpdates.put("waitList", waitListMap);

                    batch.update(eventRef, eventUpdates);

                    String eventKeyForUser = eventId;

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

                    sendLotteryNotifications(batch, eventName != null ? eventName : eventId, organizer, selectedUsers, notSelectedUsers
                    );

                    batch.commit()
                            .addOnSuccessListener(v -> onSuccess.run())
                            .addOnFailureListener(onError::accept);

                })
                .addOnFailureListener(onError::accept);
    }


    private void sendLotteryNotifications(WriteBatch batch, String eventName, String organizer, List<String> selectedUsers, List<String> notSelectedUsers) {

        //FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

        for (String user : selectedUsers) {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("content", "You have been SELECTED for " + eventName + "\ngo to event detail page to accept then invite");
            notifData.put("eventName", eventName);
            notifData.put("receiver", user);
            notifData.put("sender", organizer != null ? organizer : "System");
            notifData.put("timestamp", now);

            DocumentReference notifRef = db.collection("notification").document();
            batch.set(notifRef, notifData);
        }

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
