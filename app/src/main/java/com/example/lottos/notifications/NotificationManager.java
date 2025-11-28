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

public class NotificationManager {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "NotificationManager";

    // This is your existing NotificationModel, which we keep.
    public static class NotificationModel {
        public final String id;
        public final String content;
        public final String eventName;
        public final String receiver;
        public final String sender;
        public final String timestamp;

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

    // This is your existing NotificationCallback, which we keep.
    public interface NotificationCallback {
        void onSuccess(List<NotificationModel> list);
        void onError(Exception e);
    }

    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // + NEW METHOD FOR THE ADMIN SCREEN
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    /**
     * Fetches ALL notifications from the collection, ordered by time.
     * This is the new method for the admin user.
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

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM\ndd", Locale.getDefault());
        return sdf.format(ts.toDate()).toUpperCase(Locale.getDefault());
    }
}

