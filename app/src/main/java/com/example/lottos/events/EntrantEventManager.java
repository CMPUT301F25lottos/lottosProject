package com.example.lottos.events;

import android.util.Log;

import com.example.lottos.EventRepository;
import com.google.firebase.Timestamp;import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Manages the business logic for an entrant's interactions with events.
 *
 * Role: This class is responsible for fetching and preparing event data for presentation in the UI,
 * but contains no direct UI code itself. It handles operations such as:
 * <ul>
 *     <li>Loading all open events for general browsing.</li>
 *     <li>Loading the specific event history for a given user (e.g., waitlisted, enrolled).</li>
 *     <li>Filtering events based on keywords or availability.</li>
 * </ul>
 * It uses an EventRepository to interact with the data source and communicates results
 * back through callback interfaces.
 */
public class EntrantEventManager {
    private final EventRepository repo;
    private final FirebaseFirestore db;

    /**
     * Constructs an EntrantEventManager with a specified repository and Firestore instance.
     * @param repo The EventRepository to use for data access.
     * @param db The FirebaseFirestore instance for database operations.
     */
    public EntrantEventManager(EventRepository repo, FirebaseFirestore db) {
        this.repo = repo;
        this.db = db;
    }

    /**
     * Default constructor that initializes its own FirebaseFirestore instance
     * and a new EventRepository.
     */
    public EntrantEventManager() {
        this.db = FirebaseFirestore.getInstance();
        this.repo = new EventRepository(this.db);
    }

    /**
     * A simplified data model representing an event for display in the UI.
     * This class flattens the complex Firestore event structure into a simple POJO
     * suitable for use in adapters and UI components.
     */
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

        /**
         * Constructs a new EventModel object.
         * @param id The unique ID of the event.
         * @param name The name of the event.
         * @param isOpen A flag indicating if the event is open.
         * @param location The location of the event.
         * @param startTime A formatted string for the event's start time.
         * @param endTime A formatted string for the event's end time.
         * @param posterUrl The URL of the event's poster image.
         * @param filterWords A list of keywords for filtering.
         * @param startMillis The start time in milliseconds since the epoch.
         * @param endMillis The end time in milliseconds since the epoch.
         */
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

        /**
         * Returns the name of the event.
         * @return The event name.
         */
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Loads all events, regardless of their 'open' status. Primarily intended for
     * administrative views where a complete list of all created events is needed.
     * The results are sorted by end time in descending order.
     *
     * @param callback The callback to be invoked with the list of events or an error.
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
     * Loads all events that are currently marked as "open". This is used for the main
     * event browsing screen for entrants, showing them events they can potentially join.
     *
     * @param userName The username of the user, used for potential future logic (currently unused).
     * @param callback The callback to be invoked with the list of open events or an error.
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
     * Loads all events that a user has interacted with. This includes events they are
     * waitlisted for, selected for, enrolled in, declined, etc. It first fetches the user's
     * document to get all associated event IDs and then retrieves the details for those events.
     *
     * @param userName The username of the user whose history is being loaded.
     * @param callback The callback to be invoked with the list of historical events or an error.
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
     * A private helper method to safely extract the "filterWords" array from a Firestore document.
     * It handles cases where the field is missing or not a list of strings, returning an empty list.
     * All words are converted to lowercase for case-insensitive matching.
     *
     * @param doc The QueryDocumentSnapshot from which to extract the words.
     * @return A list of filter words, or an empty list if none are found.
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
     * Filters a given list of events based on a set of selected keywords.
     * An event is included in the result if its list of filter words contains *any* of the selected keywords.
     * If the list of selected filters is empty, the original list is returned unmodified.
     *
     * @param allEvents       The full list of events to be filtered.
     * @param selectedFilters A list of keywords chosen by the user for filtering.
     * @return A new list containing only the events that match the filter criteria.
     */
    public List<EventModel> filterEventsByKeywords(List<EventModel> allEvents,
                                                   List<String> selectedFilters) {

        if (selectedFilters == null || selectedFilters.isEmpty()) {
            return new ArrayList<>(allEvents);
        }

        List<EventModel> filteredList = new ArrayList<>();
        Set<String> filterSet = new HashSet<>();
        for (String filter : selectedFilters) {
            filterSet.add(filter.toLowerCase());
        }

        for (EventModel event : allEvents) {
            // Check for any intersection between event's keywords and selected filters
            for (String eventKeyword : event.filterWords) {
                if (filterSet.contains(eventKeyword)) {
                    filteredList.add(event);
                    break; // Found a match, add the event and move to the next one
                }
            }
        }
        return filteredList;
    }

    /**
     * Filters a list of events to include only those that overlap with a specified time range.
     * An event is considered a match if its time span [startMillis, endMillis] has any
     * overlap with the given [fromMillis, toMillis] range.
     *
     * @param events The list of events to filter.
     * @param fromMillis The start of the availability range in milliseconds since the epoch. Can be null.
     * @param toMillis The end of the availability range in milliseconds since the epoch. Can be null.
     * @return A new list containing only the events that fall within the specified availability.
     */
    public List<EventModel> filterEventsByAvailability(List<EventModel> events,
                                                       Long fromMillis,
                                                       Long toMillis) {

        if (fromMillis == null && toMillis == null) {
            return new ArrayList<>(events);
        }

        List<EventModel> out = new ArrayList<>();

        for (EventModel e : events) {
            long s = e.startMillis;
            long t = e.endMillis;

            // An event [s, t] overlaps with a filter range [fromMillis, toMillis] if:
            // The event doesn't end before the filter starts (t >= fromMillis) AND
            // The event doesn't start after the filter ends (s <= toMillis).
            boolean afterStart = (fromMillis == null || t >= fromMillis);
            boolean beforeEnd  = (toMillis == null  || s <= toMillis);

            if (afterStart && beforeEnd) {
                out.add(e);
            }
        }

        return out;
    }

    /**
     * Formats a Firebase Timestamp into a human-readable date-time string ("yyyy-MM-dd HH:mm").
     * If the timestamp is null, it returns "N/A".
     *
     * @param ts The Timestamp to format.
     * @return A formatted string representation of the timestamp or "N/A".
     */
    private String formatTimestamp(Timestamp ts) {
        if (ts == null) {
            return "N/A";
        }
        SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault()); // Use device's local timezone
        return sdf.format(ts.toDate());
    }

    /**
     * A callback interface for asynchronous event loading operations.
     */
    public interface EventsCallback {
        /**
         * Called when the event loading operation completes successfully.
         * @param events A list of event models fetched from the data source.
         * @param userWaitlistedEvents A list of event IDs for which the user is on the waitlist.
         */
        void onSuccess(List<EventModel> events, List<String> userWaitlistedEvents);
        /**
         * Called when the event loading operation fails.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }
}
