package com.example.lottos.notifications;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Manages the business logic for fetching and deleting notifications from Firestore.
 *
 * Role: This class acts as a data manager for notifications, abstracting the direct
 * interaction with the Firestore database away from the UI layer (Fragments/Activities).
 * Its responsibilities include:
 * <ul>
 *     <li>Loading all notifications for a specific user.</li>
 *     <li>Loading a complete list of all notifications for an admin view.</li>
 *     <li>Handling the deletion of a specific notification document.</li>
 *     <li>Formatting timestamp data into a user-friendly string.</li>
 * </ul>
 * It communicates results back to the caller asynchronously using a callback interface.
 */
public class NotificationManager {
    private final FirebaseFirestore db;
    private static final String TAG = "NotificationManager";

    /**
     * Default constructor that initializes its own connection to Firestore.
     */
    public NotificationManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Constructs a NotificationManager with a provided FirebaseFirestore instance.
     * This is useful for dependency injection and testing.
     * @param db The FirebaseFirestore instance to use for database operations.
     */
    public NotificationManager(FirebaseFirestore db) {
        this.db = db;
    }


    /**
     * A simplified data model representing a notification for display in the UI.
     * This class flattens the complex Firestore notification structure into a simple POJO
     * suitable for use in adapters and UI components.
     */
    public static class NotificationModel {
        public final String id;
        public final String content;
        public final String eventName;
        public final String receiver;
        public final String sender;
        public final String timestamp;

        /**
         * Constructs a new NotificationModel object.
         * @param id The unique ID of the notification.
         * @param content The main message of the notification.
         * @param eventName The name of the associated event.
         * @param receiver The username of the recipient.
         * @param sender The username of the sender.
         * @param timestamp A formatted string representing the creation time.
         */
        public NotificationModel(String id,
                                 String content,
                                 String eventName,
                                 String receiver,
                                 String sender,
                                 String timestamp) {
            this.id = id;
            this.content = content;
            this.eventName = eventName;
            this.receiver = receiver;
            this.sender = sender;
            this.timestamp = timestamp;
        }
    }

    /**
     * A callback interface for asynchronous notification loading operations.
     */
    public interface NotificationCallback {
        /**
         * Called when the notification loading operation completes successfully.
         * @param list A list of notification models fetched from the data source.
         */
        void onSuccess(List<NotificationModel> list);
        /**
         * Called when the notification loading operation fails.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }
    /**
     * Fetches ALL notifications from the collection, ordered by time.
     * This method is intended for use by an administrator to see a global view of all messages.
     * @param callback Callback to handle the full list of notifications or an error.
     */
    public void loadAllNotifications(NotificationCallback callback) {
        db.collection("notification")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Admin query returned " + queryDocumentSnapshots.size() + " documents");
                    List<NotificationModel> result = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String content = doc.getString("content");
                        String eventName = doc.getString("eventName");
                        String receiver = doc.getString("receiver");
                        String sender = doc.getString("sender");
                        Timestamp ts = doc.getTimestamp("timestamp");

                        if (content == null) content = "";
                        if (eventName == null) eventName = "";
                        if (receiver == null) receiver = "";
                        if (sender == null) sender = "";
                        if (ts == null) ts = Timestamp.now();

                        String formattedDate = formatTimestamp(ts);

                        result.add(new NotificationModel(
                                id,
                                content,
                                eventName,
                                receiver,
                                sender,
                                formattedDate
                        ));
                    }
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load all notifications for admin", e);
                    callback.onError(e);
                });
    }



    /**
     * Fetches all notifications where the specified user is the recipient.
     * This is the standard method for a regular user to view their own messages.
     * @param userName The username of the user whose notifications are to be loaded.
     * @param callback The callback to handle the list of notifications or an error.
     */
    public void loadNotificationForUser(String userName, NotificationCallback callback) {

        Log.d(TAG, "Loading notifications for userName = " + userName);

        db.collection("notification")
                .whereEqualTo("receiver", userName)
                .get()
                .addOnSuccessListener(query -> {

                    Log.d(TAG, "Query returned " + query.size() + " documents");

                    List<NotificationManager.NotificationModel> result = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {

                        String id = doc.getId();
                        String content = doc.getString("content");
                        String eventName = doc.getString("eventName");
                        String receiver = doc.getString("receiver");
                        String sender = doc.getString("sender");
                        Timestamp ts = doc.getTimestamp("timestamp");

                        Log.d(TAG, "Doc: id=" + id +
                                ", eventName=" + eventName +
                                ", receiver=" + receiver);

                        if (content == null) content = "";
                        if (eventName == null) eventName = "";
                        if (receiver == null) receiver = "";
                        if (sender == null) sender = "";
                        if (ts == null) ts = Timestamp.now();

                        String formattedDate = formatTimestamp(ts);

                        result.add(new NotificationModel(
                                id,
                                content,
                                eventName,
                                receiver,
                                sender,
                                formattedDate
                        ));
                    }

                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notifications for user " + userName, e);
                    callback.onError(e);
                });
    }

    /**
     * Deletes a single notification document from Firestore using its unique ID.
     * The `onDone` callback is always run, regardless of success or failure.
     * @param id The document ID of the notification to be deleted.
     * @param onDone A Runnable that is executed after the delete operation completes.
     */
    public void deleteNotificationById(String id, Runnable onDone) {
        db.collection("notification")
                .document(id)
                .delete()
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Deleted notification " + id);
                    onDone.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Delete failed for id: " + id, e);
                    onDone.run();
                });
    }

    /**
     * A private helper method to format a Firebase Timestamp into a short, readable
     * date string (e.g., "DEC\n01"). Returns an empty string if the timestamp is null.
     * @param ts The Timestamp to format.
     * @return A formatted date string or an empty string.
     */
    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM\ndd", Locale.getDefault());
        return sdf.format(ts.toDate()).toUpperCase(Locale.getDefault());
    }
}
