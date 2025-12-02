package com.example.lottos.organizer;

import com.example.lottos.EventRepository;
import com.example.lottos.lottery.LotterySystem;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the business logic for the detailed view of an event from an organizer's perspective.
 *
 * Role: This class is responsible for fetching all data related to a single event and performing
 * organizer-specific actions, most notably running the lottery. Its key functions are:
 * <ul>
 *     <li>Loading a complete snapshot of an event document from Firestore.</li>
 *     <li>Extracting and separating the lists of users (waitlisted, selected, enrolled, etc.) from the event data.</li>
 *     <li>Executing the lottery logic: it shuffles the waitlisted users, moves them to either the 'selected' or 'not selected' lists based on the event's capacity, and updates the event document.</li>
 *     <li>Updating the corresponding event lists in each affected user's personal document.</li>
 *     <li>Creating and sending notifications to all participants about the lottery results.</li>
 * </ul>
 * All lottery-related database modifications are performed in a single atomic Firestore WriteBatch to ensure data consistency.
 */
public class OrganizerEventDetailsManager {

    private final FirebaseFirestore db;
    private final EventRepository repo;

    /**
     * Constructs an OrganizerEventDetailsManager with a provided FirebaseFirestore instance and EventRepository.
     * This is useful for dependency injection and testing.
     * @param db The FirebaseFirestore instance to use for database operations.
     * @param repo The repository for accessing event data.
     */
    public OrganizerEventDetailsManager(FirebaseFirestore db, EventRepository repo) {
        this.db = db;
        this.repo = repo;
    }

    /**
     * Default constructor that initializes its own connection to Firestore and its own EventRepository.
     */
    public OrganizerEventDetailsManager() {
        this.db = FirebaseFirestore.getInstance();
        this.repo = new EventRepository(this.db);
    }

    /**
     * A callback interface to handle the results of loading an event's full details.
     */
    public interface LoadCallback {
        /**
         * Called on successful retrieval of all event data.
         * @param eventData A map representing the raw event document data.
         * @param waitlistUsers A list of usernames on the waitlist.
         * @param selectedUsers A list of usernames who have been selected by the lottery.
         * @param notSelectedUsers A list of usernames who were not selected by the lottery.
         * @param enrolledUsers A list of usernames who have accepted their selection and are enrolled.
         * @param cancelledUsers A list of usernames who have cancelled or were removed.
         */
        void onSuccess(Map<String, Object> eventData, List<String> waitlistUsers, List<String> selectedUsers, List<String> notSelectedUsers, List<String> enrolledUsers, List<String> cancelledUsers);
        /**
         * Called when an error occurs during data fetching.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Loads all details for a specific event, including all lists of participating users.
     * It fetches the event document and then parses the user lists.
     * @param eventId The unique identifier of the event to load.
     * @param cb The callback to be invoked with the loaded data or an error.
     */
    public void loadEvent(String eventId, LoadCallback cb) {
        DocumentReference eventDoc = repo.getEvent(eventId);

        eventDoc.get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        cb.onError(new Exception("Event not found"));
                        return;
                    }

                    Map<String, Object> data = snap.getData();

                    List<String> waitlistUsers   = extractUserList(data, "waitList");
                    List<String> selectedUsers   = extractUserList(data, "selectedList");
                    List<String> notSelectedUsers= extractUserList(data, "notSelectedList");
                    List<String> enrolledUsers   = extractUserList(data, "enrolledList");
                    List<String> cancelledUsers  = extractUserList(data, "cancelledList");

                    cb.onSuccess(data, waitlistUsers, selectedUsers, notSelectedUsers, enrolledUsers, cancelledUsers);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * A private helper method to safely extract a list of user strings from a nested map structure in the event data.
     * For example, it reads the 'users' array from the 'waitList' map.
     * @param eventData The main event data map.
     * @param key The key of the parent map (e.g., "waitList", "selectedList").
     * @return A list of usernames, or an empty list if the data is missing or malformed.
     */
    private List<String> extractUserList(Map<String, Object> eventData, String key) {
        List<String> result = new ArrayList<>();
        if (eventData == null) return result;

        Object parent = eventData.get(key);
        if (!(parent instanceof Map)) return result;

        Object listObj = ((Map<?, ?>) parent).get("users");
        if (listObj instanceof List) {
            for (Object u : (List<?>) listObj) {
                if (u != null) {
                    result.add(u.toString());
                }
            }
        }
        return result;
    }

    /**
     * Executes the lottery for a given event using the provided list of waitlisted users.
     * This method shuffles the users, assigns them to 'selected' or 'not selected' lists,
     * sends notifications, and updates all relevant documents in a single atomic batch.
     * @param eventId The ID of the event to run the lottery for.
     * @param waitUsers The list of users currently on the waitlist.
     * @param onSuccess A callback to run on successful completion of the batch write.
     * @param onError A callback to handle any exceptions that occur.
     */
    public void runLottery(String eventId, List<String> waitUsers, Runnable onSuccess, java.util.function.Consumer<Exception> onError) {

        if (waitUsers == null || waitUsers.isEmpty()) {
            onError.accept(new Exception("No users on waitlist to run lottery."));
            return;
        }

        DocumentReference eventRef = repo.getEvent(eventId);

        eventRef.get()
                .addOnSuccessListener(eventSnap -> {
                    if (!eventSnap.exists()) {
                        onError.accept(new Exception("Event not found"));
                        return;
                    }

                    String eventName   = eventSnap.getString("eventName");
                    String organizer   = eventSnap.getString("organizer");
                    Long selectionCapL = eventSnap.getLong("selectionCap");
                    int selectionCap   = (selectionCapL != null && selectionCapL > 0)
                            ? selectionCapL.intValue()
                            : waitUsers.size();

                    List<String> shuffled = new ArrayList<>(waitUsers);
                    java.util.Collections.shuffle(shuffled);

                    List<String> selectedUsers    = new ArrayList<>();
                    List<String> notSelectedUsers = new ArrayList<>();

                    for (int i = 0; i < shuffled.size(); i++) {
                        if (i < selectionCap) {
                            selectedUsers.add(shuffled.get(i));
                        } else {
                            notSelectedUsers.add(shuffled.get(i));
                        }
                    }

                    WriteBatch batch = db.batch();

                    Map<String, Object> eventUpdates = new HashMap<>();
                    eventUpdates.put("IsLottery", true);

                    Map<String, Object> selectedListMap = new HashMap<>();
                    selectedListMap.put("users", selectedUsers);
                    eventUpdates.put("selectedList", selectedListMap);

                    Map<String, Object> notSelectedListMap = new HashMap<>();
                    notSelectedListMap.put("users", notSelectedUsers);
                    eventUpdates.put("notSelectedList", notSelectedListMap);

                    Map<String, Object> waitListMap = new HashMap<>();
                    waitListMap.put("users", new ArrayList<String>());
                    eventUpdates.put("waitList", waitListMap);

                    batch.update(eventRef, eventUpdates);

                    String eventKeyForUser = eventId;

                    for (String userId : selectedUsers) {
                        DocumentReference userRef = db.collection("users").document(userId);
                        batch.update(userRef,
                                "waitListedEvents.events", FieldValue.arrayRemove(eventKeyForUser),
                                "selectedEvents.events",   FieldValue.arrayUnion(eventKeyForUser)
                        );
                    }

                    for (String userId : notSelectedUsers) {
                        DocumentReference userRef = db.collection("users").document(userId);
                        batch.update(userRef,
                                "waitListedEvents.events",  FieldValue.arrayRemove(eventKeyForUser),
                                "notSelectedEvents.events", FieldValue.arrayUnion(eventKeyForUser)
                        );
                    }

                    sendLotteryNotifications(batch, eventName != null ? eventName : eventId, organizer, selectedUsers, notSelectedUsers
                    );

                    batch.commit()
                            .addOnSuccessListener(v -> onSuccess.run())
                            .addOnFailureListener(onError::accept);

                })
                .addOnFailureListener(onError::accept);
    }
    private List<String> extractUsers(DocumentSnapshot snap, String key) {
        List<String> result = new ArrayList<>();

        Object parent = snap.get(key);
        if (!(parent instanceof Map)) {
            return result;
        }

        Map<?, ?> parentMap = (Map<?, ?>) parent;
        Object listObj = parentMap.get("users");

        if (listObj instanceof List<?>) {
            for (Object u : (List<?>) listObj) {
                if (u != null) result.add(u.toString());
            }
        }

        return result;
    }

    public void replaceDeclinedUser(String eventId, String declinedUser, Runnable onSuccess, java.util.function.Consumer<Exception> onError) {

        DocumentReference eventRef = repo.getEvent(eventId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(eventRef);

                    if (!snap.exists()) {
                        try {
                            throw new Exception("Event not found");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    List<String> selected = extractUsers(snap, "selectedList");
                    List<String> notSelected = extractUsers(snap, "notSelectedList");

                    selected.remove(declinedUser);

                    String promoted = null;
                    if (!notSelected.isEmpty()) {
                        promoted = notSelected.remove(0);
                        selected.add(promoted);
                    }

                    Map<String, Object> selectedMap = new HashMap<>();
                    selectedMap.put("users", selected);

                    Map<String, Object> notSelectedMap = new HashMap<>();
                    notSelectedMap.put("users", notSelected);

                    transaction.update(eventRef, "selectedList", selectedMap);
                    transaction.update(eventRef, "notSelectedList", notSelectedMap);

                    transaction.update(eventRef, "cancelledList.users",
                            FieldValue.arrayUnion(declinedUser));

                    if (promoted != null) {
                        DocumentReference notifRef = db.collection("notification").document();
                        Map<String, Object> data = new HashMap<>();
                        data.put("receiver", promoted);
                        data.put("eventName", snap.getString("eventName"));
                        data.put("content", "You have been selected after another user declined.");
                        data.put("timestamp", com.google.firebase.Timestamp.now());
                        data.put("sender", snap.getString("organizer"));

                        transaction.set(notifRef, data);
                    }

                    return null;

                }).addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(onError::accept);
    }

    /**
     * Adds notification creation operations to the provided WriteBatch.
     * It creates one notification for each user who was selected and one for each user who was not.
     * @param batch The Firestore WriteBatch to add the notification operations to.
     * @param eventName The name of the event.
     * @param organizer The username of the event organizer.
     * @param selectedUsers The list of users who were selected.
     * @param notSelectedUsers The list of users who were not selected.
     */
    private void sendLotteryNotifications(WriteBatch batch, String eventName, String organizer, List<String> selectedUsers, List<String> notSelectedUsers) {
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

        for (String user : selectedUsers) {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("content", "You have been SELECTED for " + eventName + "\ngo to event detail page to accept then invite");
            notifData.put("eventName", eventName);
            notifData.put("receiver", user);
            notifData.put("sender", organizer != null ? organizer : "System");
            notifData.put("timestamp", now);

            DocumentReference notifRef = db.collection("notification").document();
            batch.set(notifRef, notifData);
        }

        for (String user : notSelectedUsers) {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("content", "You were NOT selected for " + eventName);
            notifData.put("eventName", eventName);
            notifData.put("receiver", user);
            notifData.put("sender", organizer != null ? organizer : "System");
            notifData.put("timestamp", now);

            DocumentReference notifRef = db.collection("notification").document();
            batch.set(notifRef, notifData);
        }
    }


}
