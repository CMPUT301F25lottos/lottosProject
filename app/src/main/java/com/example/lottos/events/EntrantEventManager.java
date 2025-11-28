package com.example.lottos.events;

import com.example.lottos.EventRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.Query;

/**
 * Handles all business logic for entrant events.
 * - Loading events for UI
 * - Join/leave waitlist
 * No UI code here.
 */
public class EntrantEventManager {
    private final EventRepository repo = new EventRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static class EventModel {
        public String id;
        public String name;
        public boolean isOpen;
        public String location;
        public String startTime;
        public String endTime;
        public String posterUrl;

        public EventModel(String id, String name, boolean isOpen, String location, String startTime, String endTime, String posterUrl) {
            this.id = id;
            this.name = name;
            this.isOpen = isOpen;
            this.location = location;
            this.startTime = startTime;
            this.endTime = endTime;
            this.posterUrl = posterUrl;
        }

        @Override
        public String toString() {return name;}
    }

    public void loadAllOpenEvents(EventsCallback callback) {

        repo.getAllEvents().get().addOnSuccessListener(query -> {
                    List<EventModel> result = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {

                        String id = doc.getId();
                        String name = doc.getString("eventName");
                        Boolean openFlag = doc.getBoolean("IsOpen");
                        String location = doc.getString("location");

                        Timestamp startTs = doc.getTimestamp("startTime");
                        Timestamp endTs   = doc.getTimestamp("endTime");
                        String startStr = formatTimestamp(startTs);
                        String endStr   = formatTimestamp(endTs);

                        String posterUrl = doc.getString("posterUrl");

                        if (name != null && openFlag != null) {
                            result.add(new EventModel(id, name, openFlag, location, startStr, endStr, posterUrl
                            ));
                        }
                    }

                    result.sort((e1, e2) -> e2.endTime.compareTo(e1.endTime));

                    callback.onSuccess(result, new ArrayList<>());
                })
                .addOnFailureListener(e -> {
                    Log.e("EntrantEventManager", "Error loading all events for admin", e);
                    callback.onError(e);
                });
    }

    public void loadOpenEventsForUser(String userName, EventsCallback callback) {

        repo.getAllEvents().get()
                .addOnSuccessListener(query -> {
                    List<EventModel> result = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {

                        String id = doc.getId();
                        String name = doc.getString("eventName");
                        Boolean openFlag = doc.getBoolean("IsOpen");
                        String location = doc.getString("location");

                        Timestamp startTs = doc.getTimestamp("startTime");
                        Timestamp endTs   = doc.getTimestamp("endTime");

                        String startStr = formatTimestamp(startTs);
                        String endStr   = formatTimestamp(endTs);

                        String posterUrl = doc.getString("posterUrl");

                        if (name != null && openFlag != null && openFlag) {
                            result.add(new EventModel(id,name,true,location,startStr,endStr,posterUrl
                            ));
                        }
                    }
                    callback.onSuccess(result, new ArrayList<>());
                })
                .addOnFailureListener(callback::onError);
    }

    public void loadEventsHistory(String userName, EventsCallback callback) {

        db.collection("users").document(userName).get()
                .addOnSuccessListener(userSnap -> {
                    if (!userSnap.exists()) {
                        callback.onSuccess(new ArrayList<>(), new ArrayList<>());
                        return;
                    }

                    List<String> waitlistedIds = (List<String>) userSnap.get("waitListedEvents.events");
                    List<String> selectedIds = (List<String>) userSnap.get("selectedEvents.events");
                    List<String> closedIds = (List<String>) userSnap.get("closedEvents.events");
                    List<String> declinedIds = (List<String>) userSnap.get("declinedEvents.events");
                    List<String> enrolledIds = (List<String>) userSnap.get("enrolledEvents.events");
                    List<String> notSelectedIds = (List<String>) userSnap.get("notSelectedEvents.events");

                    Set<String> allIdsSet = new HashSet<>();

                    if (waitlistedIds != null) allIdsSet.addAll(waitlistedIds);
                    if (selectedIds != null) allIdsSet.addAll(selectedIds);
                    if (closedIds != null) allIdsSet.addAll(closedIds);
                    if (declinedIds != null) allIdsSet.addAll(declinedIds);
                    if (enrolledIds != null) allIdsSet.addAll(enrolledIds);
                    if (notSelectedIds != null) allIdsSet.addAll(notSelectedIds);

                    List<String> allIds = new ArrayList<>(allIdsSet);

                    if (allIds.isEmpty()) {
                        callback.onSuccess(new ArrayList<>(), allIds);
                        return;
                    }

                    repo.getAllEvents().get()
                            .addOnSuccessListener(query -> {
                                List<EventModel> result = new ArrayList<>();

                                for (QueryDocumentSnapshot doc : query) {
                                    String id = doc.getId();

                                    if (!allIds.contains(id)) continue;

                                    String name = doc.getString("eventName");
                                    Boolean openFlag = doc.getBoolean("IsOpen");
                                    String location = doc.getString("location");

                                    Timestamp startTs = doc.getTimestamp("startTime");
                                    Timestamp endTs   = doc.getTimestamp("endTime");
                                    String startStr = formatTimestamp(startTs);
                                    String endStr   = formatTimestamp(endTs);

                                    String posterUrl = doc.getString("posterUrl");

                                    if (name != null) {
                                        result.add(new EventModel(id, name, openFlag != null ? openFlag : false, location, startStr, endStr, posterUrl
                                        ));
                                    }
                                }
                                callback.onSuccess(result, allIds);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }


    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "";
        java.util.Date date = ts.toDate();
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault());
        return sdf.format(date);
    }

    public interface EventsCallback {
        void onSuccess(List<EventModel> events, List<String> userWaitlistedEvents);
        void onError(Exception e);
    }
}
