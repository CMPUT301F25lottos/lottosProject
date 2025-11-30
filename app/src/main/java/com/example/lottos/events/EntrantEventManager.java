package com.example.lottos.events;

import android.util.Log;

import com.example.lottos.EventRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles all business logic for entrant events.
 * - Loading events for UI
 * - Join/leave waitlist
 * No UI code here.
 */
public class EntrantEventManager {
    private final EventRepository repo;
    private final FirebaseFirestore db;
    public EntrantEventManager(EventRepository repo, FirebaseFirestore db) {
        this.repo = repo;
        this.db = db;
    }

    public EntrantEventManager() {
        this.db = FirebaseFirestore.getInstance();
        this.repo = new EventRepository(this.db);
    }

    public static class EventModel {
        public String id;
        public String name;
        public boolean isOpen;
        public String location;
        public String startTime;
        public String endTime;
        public String posterUrl;
        public List<String> filterWords;   // keywords for filtering
        public long startMillis;           // for availability filtering
        public long endMillis;

        public EventModel(String id,
                          String name,
                          boolean isOpen,
                          String location,
                          String startTime,
                          String endTime,
                          String posterUrl,
                          List<String> filterWords,
                          long startMillis,
                          long endMillis) {
            this.id = id;
            this.name = name;
            this.isOpen = isOpen;
            this.location = location;
            this.startTime = startTime;
            this.endTime = endTime;
            this.posterUrl = posterUrl;
            this.filterWords = (filterWords != null) ? filterWords : new ArrayList<>();
            this.startMillis = startMillis;
            this.endMillis = endMillis;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Load all events (open or closed) – used for admin view.
     */
    public void loadAllOpenEvents(EventsCallback callback) {

        repo.getAllEvents().get()
                .addOnSuccessListener(query -> {
                    List<EventModel> result = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {

                        String id = doc.getId();
                        String name = doc.getString("eventName");
                        Boolean openFlag = doc.getBoolean("IsOpen");
                        String location = doc.getString("location");
                        String posterUrl = doc.getString("posterUrl");

                        Timestamp startTs = doc.getTimestamp("startTime");
                        Timestamp endTs   = doc.getTimestamp("endTime");

                        String startStr = formatTimestamp(startTs);
                        String endStr   = formatTimestamp(endTs);

                        long startMillis = (startTs != null) ? startTs.toDate().getTime() : 0L;
                        long endMillis   = (endTs != null)   ? endTs.toDate().getTime()   : 0L;

                        List<String> filterWords = extractFilterWords(doc);

                        if (name != null && openFlag != null) {
                            result.add(new EventModel(
                                    id,
                                    name,
                                    openFlag,
                                    location,
                                    startStr,
                                    endStr,
                                    posterUrl,
                                    filterWords,
                                    startMillis,
                                    endMillis
                            ));
                        }
                    }

                    // Sort by end time (descending) as before
                    result.sort((e1, e2) -> e2.endTime.compareTo(e1.endTime));

                    callback.onSuccess(result, new ArrayList<>());
                })
                .addOnFailureListener(e -> {
                    Log.e("EntrantEventManager", "Error loading all events for admin", e);
                    callback.onError(e);
                });
    }

    /**
     * Load all open events (for entrant home screen).
     */
    public void loadOpenEventsForUser(String userName, EventsCallback callback) {

        repo.getAllEvents().get()
                .addOnSuccessListener(query -> {
                    List<EventModel> result = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {

                        String id = doc.getId();
                        String name = doc.getString("eventName");
                        Boolean openFlag = doc.getBoolean("IsOpen");
                        String location = doc.getString("location");
                        String posterUrl = doc.getString("posterUrl");

                        Timestamp startTs = doc.getTimestamp("startTime");
                        Timestamp endTs   = doc.getTimestamp("endTime");

                        String startStr = formatTimestamp(startTs);
                        String endStr   = formatTimestamp(endTs);

                        long startMillis = (startTs != null) ? startTs.toDate().getTime() : 0L;
                        long endMillis   = (endTs != null)   ? endTs.toDate().getTime()   : 0L;

                        List<String> filterWords = extractFilterWords(doc);

                        if (name != null && openFlag != null && openFlag) {
                            result.add(new EventModel(
                                    id,
                                    name,
                                    true,
                                    location,
                                    startStr,
                                    endStr,
                                    posterUrl,
                                    filterWords,
                                    startMillis,
                                    endMillis
                            ));
                        }
                    }
                    callback.onSuccess(result, new ArrayList<>());
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Load all events that are in the user's history (waitlisted, selected, etc.).
     */
    public void loadEventsHistory(String userName, EventsCallback callback) {

        db.collection("users").document(userName).get()
                .addOnSuccessListener(userSnap -> {
                    if (!userSnap.exists()) {
                        callback.onSuccess(new ArrayList<>(), new ArrayList<>());
                        return;
                    }

                    List<String> waitlistedIds  = (List<String>) userSnap.get("waitListedEvents.events");
                    List<String> selectedIds    = (List<String>) userSnap.get("selectedEvents.events");
                    List<String> closedIds      = (List<String>) userSnap.get("closedEvents.events");
                    List<String> declinedIds    = (List<String>) userSnap.get("declinedEvents.events");
                    List<String> enrolledIds    = (List<String>) userSnap.get("enrolledEvents.events");
                    List<String> notSelectedIds = (List<String>) userSnap.get("notSelectedEvents.events");

                    Set<String> allIdsSet = new HashSet<>();

                    if (waitlistedIds != null)   allIdsSet.addAll(waitlistedIds);
                    if (selectedIds != null)     allIdsSet.addAll(selectedIds);
                    if (closedIds != null)       allIdsSet.addAll(closedIds);
                    if (declinedIds != null)     allIdsSet.addAll(declinedIds);
                    if (enrolledIds != null)     allIdsSet.addAll(enrolledIds);
                    if (notSelectedIds != null)  allIdsSet.addAll(notSelectedIds);

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
                                    String posterUrl = doc.getString("posterUrl");

                                    Timestamp startTs = doc.getTimestamp("startTime");
                                    Timestamp endTs   = doc.getTimestamp("endTime");

                                    String startStr = formatTimestamp(startTs);
                                    String endStr   = formatTimestamp(endTs);

                                    long startMillis = (startTs != null) ? startTs.toDate().getTime() : 0L;
                                    long endMillis   = (endTs != null)   ? endTs.toDate().getTime()   : 0L;

                                    List<String> filterWords = extractFilterWords(doc);

                                    if (name != null) {
                                        result.add(new EventModel(
                                                id,
                                                name,
                                                openFlag != null ? openFlag : false,
                                                location,
                                                startStr,
                                                endStr,
                                                posterUrl,
                                                filterWords,
                                                startMillis,
                                                endMillis
                                        ));
                                    }
                                }
                                callback.onSuccess(result, allIds);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Helper: extract "filterWords" array from a Firestore document as List<String>.
     */
    private List<String> extractFilterWords(QueryDocumentSnapshot doc) {
        List<String> filterWords = new ArrayList<>();
        Object fwObj = doc.get("filterWords");
        if (fwObj instanceof List<?>) {
            for (Object o : (List<?>) fwObj) {
                if (o instanceof String) {
                    filterWords.add(((String) o).toLowerCase());
                }
            }
        }
        return filterWords;
    }

    /**
     * Filters a list of events based on selected keywords.
     * Returns events whose filterWords list contains ANY of the selected filters.
     *
     * @param allEvents       full list of events (e.g., loaded from loadOpenEventsForUser)
     * @param selectedFilters keywords chosen by the user
     * @return filtered subset of events
     */
    public List<EventModel> filterEventsByKeywords(List<EventModel> allEvents,
                                                   List<String> selectedFilters) {

        if (selectedFilters == null || selectedFilters.isEmpty()) {
            // no filters -> return everything
            return new ArrayList<>(allEvents);
        }

        // normalize selected filters to lowercase
        List<String> normalized = new ArrayList<>();
        for (String f : selectedFilters) {
            if (f != null && !f.trim().isEmpty()) {
                normalized.add(f.trim().toLowerCase());
            }
        }
        if (normalized.isEmpty()) {
            return new ArrayList<>(allEvents);
        }

        List<EventModel> out = new ArrayList<>();

        for (EventModel e : allEvents) {
            if (e.filterWords == null || e.filterWords.isEmpty()) continue;

            // include event if it matches ANY of the selected filters
            for (String f : normalized) {
                if (e.filterWords.contains(f)) {
                    out.add(e);
                    break; // prevent duplicates
                }
            }
        }

        return out;
    }

    /**
     * Filters events so that only those within the given availability range remain.
     * If fromMillis or toMillis is null, that bound is ignored.
     */
    public List<EventModel> filterEventsByAvailability(List<EventModel> events,
                                                       Long fromMillis,
                                                       Long toMillis) {

        if (fromMillis == null && toMillis == null) {
            // no availability filter -> return everything
            return new ArrayList<>(events);
        }

        List<EventModel> out = new ArrayList<>();

        for (EventModel e : events) {
            long s = e.startMillis;
            long t = e.endMillis;

            boolean afterStart = (fromMillis == null || t >= fromMillis);
            boolean beforeEnd  = (toMillis == null  || s <= toMillis);

            if (afterStart && beforeEnd) {
                out.add(e);
            }
        }

        return out;
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
