package com.example.lottos.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentSendNotificationScreenBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendNotificationScreen extends Fragment {

    private FragmentSendNotificationScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;
    private boolean isAdmin = false;

    private final List<String> eventNames = new ArrayList<>();
    private final List<String> eventIds = new ArrayList<>();
    private ArrayAdapter<String> eventAdapter;
    private final List<String> groups = List.of("waitList", "selectedList", "cancelledList");
    private ArrayAdapter<String> groupAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSendNotificationScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = SendNotificationScreenArgs.fromBundle(getArguments()).getUserName();
        db = FirebaseFirestore.getInstance();

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);

        setupAdapters();
        loadEvents();
        setupNavButtons();

        binding.btnSendMessage.setOnClickListener(v -> sendNotification());
    }

    private void setupAdapters() {
        eventAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, eventNames);
        eventAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spEventSelect.setAdapter(eventAdapter);

        groupAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, groups);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spGroupSelect.setAdapter(groupAdapter);
    }

    private void loadEvents() {
        Query query;
        if (isAdmin) {
            query = db.collection("open events");
        } else {
            query = db.collection("open events").whereEqualTo("organizer", userName);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventIds.clear();
                    eventNames.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("eventName");

                        if (name == null || name.isEmpty()) {
                            name = "(Unnamed Event: " + id.substring(0, 5) + "...)";
                        }

                        eventIds.add(id);
                        eventNames.add(name);
                    }

                    if (eventIds.isEmpty()) {
                        String message = isAdmin ? "There are no events in the system." : "You have no events to manage.";
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        binding.btnSendMessage.setEnabled(false);
                    } else {
                        binding.btnSendMessage.setEnabled(true);
                    }

                    eventAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed loading events", e);
                    Toast.makeText(requireContext(), "Error loading events.", Toast.LENGTH_SHORT).show();
                });
    }


    private void sendNotification() {
        if (eventIds.isEmpty()) {
            Toast.makeText(requireContext(), "You have no events to send notifications for.", Toast.LENGTH_SHORT).show();
            return;
        }

        int eventIndex = binding.spEventSelect.getSelectedItemPosition();
        String group = binding.spGroupSelect.getSelectedItem().toString();
        String message = binding.etMessageContent.getText().toString().trim();

        if (eventIndex < 0 || message.isEmpty()) {
            Toast.makeText(requireContext(), "Please select an event and write a message.", Toast.LENGTH_SHORT).show();
            return;
        }

        String eventId = eventIds.get(eventIndex);
        String eventName = eventNames.get(eventIndex);

        binding.btnSendMessage.setEnabled(false);

        db.collection("open events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> groupData = (Map<String, Object>) doc.get(group);

                    if (groupData == null) {
                        Toast.makeText(requireContext(), "This group does not exist for the selected event.", Toast.LENGTH_SHORT).show();
                        binding.btnSendMessage.setEnabled(true);
                        return;
                    }

                    List<String> users = (List<String>) groupData.get("users");

                    if (users == null || users.isEmpty()) {
                        Toast.makeText(requireContext(), "No users in this group.", Toast.LENGTH_SHORT).show();
                        binding.btnSendMessage.setEnabled(true);
                        return;
                    }

                    int successCount = 0;
                    for (String receiver : users) {
                        Map<String, Object> notif = new HashMap<>();
                        notif.put("content", message);
                        notif.put("eventName", eventName);
                        notif.put("receiver", receiver);

                        // ===================================================================
                        // THIS IS THE CORRECTED LINE THAT WAS MISSING
                        // If the user is an admin, the sender is "Admin".
                        // Otherwise, it's their personal username.
                        notif.put("sender", isAdmin ? "Admin" : userName);
                        // ===================================================================

                        notif.put("timestamp", FieldValue.serverTimestamp());

                        db.collection("notification")
                                .add(notif)
                                .addOnSuccessListener(x -> Log.d("Firestore", "Sent to " + receiver))
                                .addOnFailureListener(e -> Log.e("Firestore", "Failed to send to " + receiver, e));
                        successCount++;
                    }

                    Toast.makeText(requireContext(),
                            "Message sent to " + successCount + " users.",
                            Toast.LENGTH_SHORT
                    ).show();

                    binding.etMessageContent.setText("");
                    binding.btnSendMessage.setEnabled(true);

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error sending message.", Toast.LENGTH_SHORT).show();
                    binding.btnSendMessage.setEnabled(true);
                });
    }


    private void setupNavButtons() {
        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(SendNotificationScreenDirections.actionSendNotificationScreenToNotificationScreen(userName))
        );

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(SendNotificationScreenDirections.actionSendNotificationScreenToHomeScreen(userName))
        );

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(SendNotificationScreenDirections.actionSendNotificationScreenToProfileScreen(userName))
        );

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(SendNotificationScreenDirections.actionSendNotificationScreenToEventHistoryScreen(userName))
        );

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(SendNotificationScreenDirections.actionSendNotificationScreenToOrganizerEventsScreen(userName))
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
