package com.example.lottos.events;

import static androidx.test.InstrumentationRegistry.getContext;
import static java.lang.Boolean.TRUE;

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

    /**
     * Data model for UI
     */
    public static class EventModel {
        public String id;
        public String name;
        public boolean isOpen;
        public String location;
        public String startTime;
        public String endTime;

        public EventModel(String id,
                          String name,
                          boolean isOpen,
                          String location,
                          String startTime,
                          String endTime) {
            this.id = id;
            this.name = name;
            this.isOpen = isOpen;
            this.location = location;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public String toString() {
            return name;
        }
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
                        Timestamp endTs = doc.getTimestamp("endTime");
                        String startStr = formatTimestamp(startTs);
                        String endStr = formatTimestamp(endTs);


                        if (name != null && openFlag != null) {
                            result.add(new EventModel(
                                    id,
                                    name,
                                    openFlag,
                                    location,
                                    startStr,
                                    endStr
                            ));
                        }
                    }

                    result.sort((e1, e2) -> {

                        return e2.endTime.compareTo(e1.endTime);
                    });

                    callback.onSuccess(result, new ArrayList<>());
                })
                .addOnFailureListener(e -> {
                    Log.e("EntrantEventManager", "Error loading all events for admin", e);
                    callback.onError(e);
                });
    }


    /**
     * Callback interface for loading events
     */
    public interface EventsCallback {
        void onSuccess(List<EventModel> events, List<String> userWaitlistedEvents);

        void onError(Exception e);
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "";
        java.util.Date date = ts.toDate();
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Load all open events + user's waitlisted events
     */
    public void loadOpenEventsForUser(String userName, EventsCallback callback) {

        // Directly load all open events
        repo.getAllEvents().get()
                .addOnSuccessListener(query -> {
                    List<EventModel> result = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {
                        String id = doc.getId();
                        String name = doc.getString("eventName");
                        Boolean openFlag = doc.getBoolean("IsOpen"); // you can delete this if not needed
                        String location = doc.getString("location");

                        Timestamp startTs = doc.getTimestamp("startTime");
                        Timestamp endTs = doc.getTimestamp("endTime");

                        String startStr = formatTimestamp(startTs);
                        String endStr = formatTimestamp(endTs);

                        if (name != null && openFlag != null && openFlag) {   // <-- remove the openFlag filter
                            result.add(new EventModel(
                                    id,
                                    name,
                                    openFlag != null ? openFlag : false, // or remove completely
                                    location,
                                    startStr,
                                    endStr
                            ));
                        }
                    }

                    callback.onSuccess(result, new ArrayList<>()); // empty waitlist
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

                    List<String> waitlistedIds =
                            (List<String>) userSnap.get("waitListedEvents.events");

                    List<String> selectedIds =
                            (List<String>) userSnap.get("selectedEvents.events");

                    List<String> closedIds =
                            (List<String>) userSnap.get("closedEvents.events");

                    List<String> declinedId =
                            (List<String>) userSnap.get("declinedEvents.events");

                    List<String> enrolledId =
                            (List<String>) userSnap.get("enrolledEvents.events");

                    List<String> notSelectedId =
                            (List<String>) userSnap.get("notSelectedEvents.events");

                    Set<String> allIdsSet = new HashSet<>();
                    if (waitlistedIds != null) allIdsSet.addAll(waitlistedIds);
                    if (selectedIds != null) allIdsSet.addAll(selectedIds);
                    if (closedIds != null) allIdsSet.addAll(closedIds);
                    if (declinedId != null) allIdsSet.addAll(declinedId);
                    if (enrolledId != null) allIdsSet.addAll(enrolledId);
                    if (notSelectedId != null) allIdsSet.addAll(notSelectedId);


                    List<String> allIds = new ArrayList<>(allIdsSet);

                    if (allIds.isEmpty()) {// Nothing in history
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

                                    if (name != null) {
                                        result.add(new EventModel(
                                                id,
                                                name,
                                                openFlag != null ? openFlag : false,
                                                location,
                                                startStr,
                                                endStr
                                        ));
                                    }
                                }

                                // 4) Pass union of IDs back as second param
                                callback.onSuccess(result, allIds);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

}