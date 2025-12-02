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

/**
 * A utility class responsible for performing automated cleanup tasks related to user statuses in events.
 *
 * Role: This class contains the logic for a "sweep" operation that finds users who have been
 * selected for an event but did not respond (accept/decline) before the event's start time.
 * It automatically moves these users from the 'selected' list to a 'cancelled' list and
 * sends them a notification explaining the action. This helps maintain data integrity and
 * automates a common administrative task. All operations are performed in a single Firestore
 * batch write to ensure atomicity.
 */
public class UserStatusUpdater {

    private static final String TAG = "UserStatusUpdater";

    private final FirebaseFirestore db;
    private final CollectionReference eventsRef;
    private final CollectionReference notificationsRef;

    /**
     * Default constructor that initializes its own connection to Firestore.
     */
    public UserStatusUpdater() {
        this.db = FirebaseFirestore.getInstance();
        this.eventsRef = db.collection("open events");
        this.notificationsRef = db.collection("notification");
    }

    /**
     * Constructs a UserStatusUpdater with a provided FirebaseFirestore instance.
     * This is useful for dependency injection and testing purposes.
     * @param db The FirebaseFirestore instance to use for all database operations.
     */
    public UserStatusUpdater(FirebaseFirestore db) {
        this.db = db;
        this.eventsRef = db.collection("open events");
        this.notificationsRef = db.collection("notification");
    }

    /**
     * A callback interface to report the result of the sweep operation.
     */
    public interface UpdateListener {
        /**
         * Called when the sweep process completes successfully.
         * @param updatedCount The total number of users whose status was changed.
         */
        void onUpdateSuccess(int updatedCount);
        /**
         * Called when the sweep process fails.
         * @param errorMessage A message describing the failure.
         */
        void onUpdateFailure(String errorMessage);
    }

    /**
     * Finds all events that have already started and moves any users who are still in the
     * 'selectedList' to the 'cancelledList'.
     *
     * This method performs the following actions in a single batch write:
     *  1. Queries for all events where the `startTime` is before the current time.
     *  2. For each expired event, it checks if the `selectedList.users` array is non-empty.
     *  3. If there are users, it moves them from `selectedList.users` to `cancelledList.users`.
     *  4. It clears the `selectedList.users` array.
     *  5. It creates a new notification document for each affected user, informing them of the automatic cancellation.
     *  6. Commits all these changes at once.
     *
     * The result of the operation is reported back through the provided listener.
     *
     * @param listener The listener to be notified of the operation's success or failure.
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
                            continue;
                        }

                        Log.d(TAG, "Event " + doc.getId() +
                                " has " + selectedUsers.size() + " selected users to move.");


                        batch.update(
                                eventDocRef,
                                "cancelledList.users",
                                FieldValue.arrayUnion(selectedUsers.toArray())
                        );



                        batch.update(
                                eventDocRef,
                                "selectedList.users",
                                new ArrayList<String>()
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
