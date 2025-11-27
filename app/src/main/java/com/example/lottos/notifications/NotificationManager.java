package com.example.lottos.notifications;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationManager {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "NotificationManager";

    // Model returned from manager to the screen
    public static class NotificationModel {
        public final String id;
        public final String content;
        public final String eventName;
        public final String receiver;
        public final String sender;
        public final String timestamp;  // already formatted string

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

    public interface NotificationCallback {
        void onSuccess(List<NotificationModel> list);
        void onError(Exception e);
    }

    public void loadNotificationForUser(String userName,
                                        NotificationCallback callback) {

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

                        // if you format to String here:
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
                    onDone.run(); // still call so UI can react
                });
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM\ndd", Locale.getDefault());
        return sdf.format(ts.toDate()).toUpperCase(Locale.getDefault());
    }


}
