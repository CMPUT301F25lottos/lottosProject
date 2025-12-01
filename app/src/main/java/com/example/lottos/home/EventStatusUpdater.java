package com.example.lottos.home;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Handles updating the "IsOpen" status of events based on their registration deadline.
 *
 * Role: This is a pure logic class designed to perform a specific background task.
 * Its responsibilities are:
 *  - Fetching all documents from the "open events" collection in Firestore.
 *  - For each event, comparing the current time with its "registerEndTime" timestamp.
 *  - Updating the "IsOpen" boolean field to false if the registration deadline has passed.
 *  - Reporting the outcome of the operation (success with a count of updated events, or failure)
 *    through a callback interface.
 */
public class EventStatusUpdater {

    private static final String TAG = "EventStatusUpdater";
    private final CollectionReference eventsRef;

    /**
     * Constructs an EventStatusUpdater with a provided FirebaseFirestore instance.
     * This is useful for dependency injection and testing.
     * @param db The FirebaseFirestore instance to use for database operations.
     */
    public EventStatusUpdater(FirebaseFirestore db) {
        this.eventsRef = db.collection("open events");
    }

    /**
     * A callback interface to report the result of the update operation.
     */
    public interface UpdateListener {
        /**
         * Called when the update process completes successfully.
         * @param updatedCount The number of events whose status was changed.
         */
        void onUpdateSuccess(int updatedCount);
        /**
         * Called when the update process fails.
         * @param errorMessage A message describing the failure.
         */
        void onUpdateFailure(String errorMessage);
    }

    /**
     * Default constructor that initializes its own connection to Firestore.
     */
    public EventStatusUpdater() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("open events");
    }


    /**
     * Initiates the process of checking and updating the status of all events.
     * It fetches all documents, compares their registration deadline with the current time,
     * and updates the 'IsOpen' field if it's inconsistent.
     *
     * @param listener The listener to be notified of the operation's success or failure.
     */
    public void updateEventStatuses(UpdateListener listener) {

        eventsRef.get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot.isEmpty()) {
                listener.onUpdateSuccess(0);
                return;
            }

            Timestamp nowTs = Timestamp.now();
            int updated = 0;

            for (DocumentSnapshot doc : querySnapshot) {

                Timestamp registerEnd = doc.getTimestamp("registerEndTime");
                if (registerEnd == null) continue;

                boolean shouldBeOpen = nowTs.compareTo(registerEnd) < 0;
                Boolean current = doc.getBoolean("IsOpen");

                if (current == null || current != shouldBeOpen) {
                    doc.getReference().update("IsOpen", shouldBeOpen)
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Failed updating event " + doc.getId(), e));
                    updated++;
                }
            }

            listener.onUpdateSuccess(updated);

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching open events", e);
            listener.onUpdateFailure("Failed to update event statuses.");
        });
    }
}
