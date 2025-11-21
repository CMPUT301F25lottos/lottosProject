package com.example.lottos;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentNotificationScreenBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fragment that displays user notifications.
 *
 * Role: Coordinates reading notification-related data from Firestore, formats it into
 * user-friendly text and displays it in separate "received" and "sent" lists with
 * navigation back to the home screen or to the send-notification screen.
 *
 * Responsibilities:
 * - Displays both automatically generated event notifications (lottery results)
 *   and manually sent messages stored in Firestore.
 * - Separates notifications into received and sent lists.
 * - Allows navigation to send new notifications or return to the home screen.
 */
public class NotificationScreen extends Fragment {

    private FragmentNotificationScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;

    // Use local lists for processing, then update the main lists and adapter
    private final List<String> receivedNotifications = new ArrayList<>();
    private final List<String> sentNotifications = new ArrayList<>();

    private ArrayAdapter<String> receivedAdapter;
    private ArrayAdapter<String> sentAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotificationScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NotificationScreenArgs args = NotificationScreenArgs.fromBundle(getArguments());
        userName = args.getUserName();

        db = FirebaseFirestore.getInstance();

        // navigation
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(NotificationScreen.this)
                        .navigate(NotificationScreenDirections.actionNotificationScreenToHomeScreen(userName))
        );
        binding.btnSendNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(NotificationScreen.this)
                        .navigate(NotificationScreenDirections.actionNotificationScreenToSendNotificationScreen(userName))
        );

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(NotificationScreen.this)
                        .navigate(NotificationScreenDirections.actionNotificationScreenToProfileScreen(userName))
        );

        // adapters
        receivedAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                receivedNotifications
        );
        binding.lvReceivedNotication.setAdapter(receivedAdapter);

        sentAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                sentNotifications
        );
        binding.lvSentNotication.setAdapter(sentAdapter);

        // Clear previous data and load both sections
        receivedNotifications.clear();
        sentNotifications.clear();
        fetchReceivedNotifications();
        fetchFirestoreNotifications();
    }

    /**
     * Gets auto-generated invite/uninvite messages from user document.
     * Now resolves eventId -> eventName.
     */
    private void fetchReceivedNotifications() {
        db.collection("users")
                .document(userName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists() || getActivity() == null) return;

                    Map<String, Object> selectedMap =
                            (Map<String, Object>) documentSnapshot.get("selectedEvents");
                    Map<String, Object> unselectedMap =
                            (Map<String, Object>) documentSnapshot.get("notSelectedEvents");

                    List<String> selectedList = (selectedMap != null) ? (List<String>) selectedMap.get("events") : new ArrayList<>();
                    List<String> unselectedList = (unselectedMap != null) ? (List<String>) unselectedMap.get("events") : new ArrayList<>();

                    if (selectedList == null) selectedList = new ArrayList<>();
                    if (unselectedList == null) unselectedList = new ArrayList<>();

                    int totalEvents = selectedList.size() + unselectedList.size();

                    if (totalEvents == 0) {
                        if (receivedNotifications.isEmpty()) {
                            receivedNotifications.add("No auto notifications.");
                            receivedAdapter.notifyDataSetChanged();
                        }
                        return;
                    }

                    AtomicInteger counter = new AtomicInteger(0);
                    List<String> autoNotifications = new ArrayList<>();

                    for (String eventId : selectedList) {
                        resolveEventName(eventId, eventName -> {
                            autoNotifications.add("🎉 Congrats! You're selected for " + eventName);
                            if (counter.incrementAndGet() == totalEvents) {
                                // Last event processed, update UI
                                receivedNotifications.addAll(autoNotifications);
                                receivedAdapter.notifyDataSetChanged();
                            }
                        });
                    }

                    for (String eventId : unselectedList) {
                        resolveEventName(eventId, eventName -> {
                            autoNotifications.add("😢 Sorry! You're not selected for " + eventName);
                            if (counter.incrementAndGet() == totalEvents) {
                                // Last event processed, update UI
                                receivedNotifications.addAll(autoNotifications);
                                receivedAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error loading selected/unselected", e));
    }


    /**
     * Pulls stored notifications where user is sender OR receiver.
     * The notification documents store eventId in the "eventName" field; we resolve to real name.
     */
    private void fetchFirestoreNotifications() {
        // received messages
        db.collection("notification")
                .whereEqualTo("receiver", userName)
                .get()
                .addOnSuccessListener(query -> {
                    if (getActivity() == null) return; // Fragment is not attached
                    List<String> newReceived = new ArrayList<>();
                    int total = query.size();
                    if (total == 0) {
                        receivedAdapter.notifyDataSetChanged();
                        return;
                    }
                    AtomicInteger counter = new AtomicInteger(0);

                    for (QueryDocumentSnapshot doc : query) {
                        String content = doc.getString("content");
                        String eventId = doc.getString("eventName"); // actually eventId
                        if (content == null) content = "";

                        if (eventId == null || eventId.isEmpty()) {
                            newReceived.add("📩 " + content);
                            if (counter.incrementAndGet() == total) {
                                receivedNotifications.addAll(newReceived);
                                receivedAdapter.notifyDataSetChanged();
                            }
                        } else {
                            final String finalContent = content;
                            resolveEventName(eventId, eventName -> {
                                newReceived.add("📩 " + eventName + ": " + finalContent);
                                if (counter.incrementAndGet() == total) {
                                    receivedNotifications.addAll(newReceived);
                                    receivedAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Failed to load received notifications", e));

        // sent messages
        db.collection("notification")
                .whereEqualTo("sender", userName)
                .get()
                .addOnSuccessListener(query -> {
                    if (getActivity() == null) return;
                    sentNotifications.clear();

                    if (query.isEmpty()) {
                        sentNotifications.add("No sent notifications.");
                        sentAdapter.notifyDataSetChanged();
                        return;
                    }

                    List<String> newSent = new ArrayList<>();
                    int total = query.size();
                    AtomicInteger counter = new AtomicInteger(0);

                    for (QueryDocumentSnapshot doc : query) {
                        String content = doc.getString("content");
                        String eventId = doc.getString("eventName"); // actually eventId
                        if (content == null) content = "";

                        if (eventId == null || eventId.isEmpty()) {
                            newSent.add("📤 " + content);
                            if (counter.incrementAndGet() == total) {
                                sentNotifications.addAll(newSent);
                                sentAdapter.notifyDataSetChanged();
                            }
                        } else {
                            final String finalContent = content;
                            resolveEventName(eventId, eventName -> {
                                newSent.add("📤 " + eventName + ": " + finalContent);
                                if (counter.incrementAndGet() == total) {
                                    sentNotifications.addAll(newSent);
                                    sentAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Failed to load sent notifications", e));
    }


    /**
     * Helper to resolve an eventId to its eventName from /open events/{eventId}.
     * Falls back to the eventId itself if lookup fails or name missing.
     */
    private void resolveEventName(String eventId, EventNameCallback callback) {
        db.collection("open events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    String name = null;
                    if (doc.exists()) {
                        name = doc.getString("eventName");
                    }
                    if (name == null || name.trim().isEmpty()) {
                        name = eventId; // fallback to ID
                    }
                    callback.onNameResolved(name);
                })
                .addOnFailureListener(e -> callback.onNameResolved(eventId)); // fallback to ID on error
    }

    private interface EventNameCallback {
        void onNameResolved(String eventName);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
