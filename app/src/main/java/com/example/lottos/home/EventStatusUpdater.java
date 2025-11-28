package com.example.lottos.home;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Handles updating event "IsOpen" statuses based on RegisterEnd timestamp.
 *
 * Role: Pure logic class for Firestore-driven event updates.
 * Responsibilities:
 *  - Fetch all events from "open events"
 *  - Compare timestamps
 *  - Update "IsOpen" only if necessary
 *  - Return results through callback
 */
public class EventStatusUpdater {

    private static final String TAG = "EventStatusUpdater";
    private final CollectionReference eventsRef;

    public interface UpdateListener {
        void onUpdateSuccess(int updatedCount);
        void onUpdateFailure(String errorMessage);
    }

    public EventStatusUpdater() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("open events");
    }

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
