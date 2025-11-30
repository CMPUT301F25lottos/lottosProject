package com.example.lottos.home;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserStatusUpdater {

    private static final String TAG = "UserStatusUpdater";

    private final FirebaseFirestore db;
    private final CollectionReference eventsRef;
    private final CollectionReference notificationsRef;
    public UserStatusUpdater() {
        this.db = FirebaseFirestore.getInstance();
        this.eventsRef = db.collection("open events");
        this.notificationsRef = db.collection("notification");
    }

    public UserStatusUpdater(FirebaseFirestore db) {
        this.db = db;
        this.eventsRef = db.collection("open events");
        this.notificationsRef = db.collection("notification");
    }
    public interface UpdateListener {
        void onUpdateSuccess(int updatedCount);
        void onUpdateFailure(String errorMessage);
    }

    /**
     * When called:
     *  - Find all events with startTime < now
     *  - For each event:
     *      if selectedList.users is non-empty:
     *          move ALL those users to cancelledList.users,
     *          clear selectedList.users,
     *          send each user a notification.
     */
    public void sweepExpiredSelectedUsers(UpdateListener listener) {

        Timestamp now = Timestamp.now();
        Log.d(TAG, "sweepExpiredSelectedUsers CALLED at " + now.toDate());

        eventsRef
                .whereLessThan("startTime", now)
                .get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    Log.d(TAG, "Expired events found: " + querySnapshot.size());

                    if (querySnapshot.isEmpty()) {
                        if (listener != null) listener.onUpdateSuccess(0);
                        return;
                    }

                    WriteBatch batch = db.batch();
                    int affectedUsersCount = 0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        DocumentReference eventDocRef = doc.getReference();

                        String eventName = doc.getString("eventName");
                        if (eventName == null) eventName = "this event";
                        String organizer = doc.getString("organizer");


                        Map<String, Object> selectedList =
                                (Map<String, Object>) doc.get("selectedList");
                        if (selectedList == null) continue;

                        List<String> selectedUsers =
                                (List<String>) selectedList.get("users");
                        if (selectedUsers == null || selectedUsers.isEmpty()) {
                            continue; // nothing to do for this event
                        }

                        Log.d(TAG, "Event " + doc.getId() +
                                " has " + selectedUsers.size() + " selected users to move.");


                        batch.update(
                                eventDocRef,
                                "cancelledList.users",
                                FieldValue.arrayUnion(selectedUsers.toArray(new String[0]))
                        );


                        batch.update(
                                eventDocRef,
                                "selectedList.users",
                                new ArrayList<String>()  // []
                        );


                        for (String userId : selectedUsers) {
                            DocumentReference notifRef = notificationsRef.document();
                            Map<String, Object> notifData = new HashMap<>();
                            notifData.put("receiver", userId);
                            notifData.put("sender", organizer);
                            notifData.put("eventName", eventName);
                            notifData.put("timestamp", Timestamp.now());
                            notifData.put(
                                    "content",
                                    "You were removed from the selected list for " + eventName +
                                            " because the event has started and you did not respond in time."
                            );
                            notifData.put("type", "AUTO_CANCELLED_SELECTION");

                            batch.set(notifRef, notifData);
                            affectedUsersCount++;
                        }
                    }

                    int finalAffectedUsers = affectedUsersCount;

                    // If nothing to update, just return success(0)
                    if (finalAffectedUsers == 0) {
                        Log.d(TAG, "No selected users to move for any expired events.");
                        if (listener != null) listener.onUpdateSuccess(0);
                        return;
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Sweep success. Moved " + finalAffectedUsers +
                                        " users from selected -> cancelled.");
                                if (listener != null) listener.onUpdateSuccess(finalAffectedUsers);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Sweep FAILED: ", e);
                                if (listener != null) listener.onUpdateFailure(e.getMessage());
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Query for expired events FAILED: ", e);
                    if (listener != null) listener.onUpdateFailure(e.getMessage());
                });
    }
}
