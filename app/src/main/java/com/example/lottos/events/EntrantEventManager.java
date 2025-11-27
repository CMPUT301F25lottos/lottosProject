package com.example.lottos.events;

import com.example.lottos.EventRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.util.Log;
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

    /** Data model for UI */
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








    /** Callback interface for loading events */
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

    /** Load all open events + user's waitlisted events */
    public void loadEventsForUser(String userName, EventsCallback callback) {
        // Step 1: load user waitlisted events
        db.collection("users").document(userName).get()
                .addOnSuccessListener(userSnap -> {

                    final List<String> waitlisted = new ArrayList<>();
                    Object data = userSnap.get("waitListedEvents.events");
                    if (data instanceof List) {
                        // This is safe because we are modifying the list's contents,
                        // not reassigning the 'waitlisted' variable itself.
                        waitlisted.addAll((List<String>) data);
                    }

                    // Step 2: load all events
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

                                    if (name != null && openFlag != null && openFlag) {
                                        result.add(new EventModel(
                                                id,
                                                name,
                                                true,        // isOpen
                                                location,
                                                startStr,
                                                endStr
                                        ));
                                    }
                                }
                                callback.onSuccess(result, waitlisted);
                            })
                            .addOnFailureListener(callback::onError);

                }
                ).addOnFailureListener(callback::onError);
    }
}

