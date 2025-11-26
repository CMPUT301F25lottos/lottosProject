package com.example.lottos.organizer;

import com.example.lottos.EventRepository;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Non-UI logic for OrganizerEventDetailsScreen.
 * Loads event metadata + all participant lists for an eventId.
 *
 * Lists (from Firestore):
 * - waitList.users
 * - selectedList.users
 * - notSelectedList.users
 * - enrolledList.users
 * - cancelledList.users
 */
public class OrganizerEventDetailsManager {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final EventRepository repo = new EventRepository();

    public interface LoadCallback {
        void onSuccess(Map<String, Object> eventData,
                       List<String> waitlistUsers,
                       List<String> selectedUsers,
                       List<String> notSelectedUsers,
                       List<String> enrolledUsers,
                       List<String> cancelledUsers);

        void onError(Exception e);
    }

    /**
     * Loads a single event by document ID (eventId).
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

                    cb.onSuccess(data,
                            waitlistUsers,
                            selectedUsers,
                            notSelectedUsers,
                            enrolledUsers,
                            cancelledUsers);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Helper for Firestore structure of the form:
     *   "<key>": { "users": [ "u1", "u2", ... ] }
     */
    @SuppressWarnings("unchecked")
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
}
