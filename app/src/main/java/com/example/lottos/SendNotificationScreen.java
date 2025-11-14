package com.example.lottos;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentSendNotificationScreenBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This fragment  allows organizers to send custom notifications to groups of users
 * associated with their events.
 *
 * Role:
 * This fragment provides organizers with a simple interface to select one of their
 * organized events, choose a recipient group (e.g, waitlisted, selected, cancelled),
 * compose a message, and broadcast it to all users in that group through Firestore.
 * It also manages navigation back to the NotificationScreen.
 */

public class SendNotificationScreen extends Fragment {

    private FragmentSendNotificationScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;

    private List<String> eventList = new ArrayList<>();
    private ArrayAdapter<String> eventAdapter;

    private List<String> groupList = new ArrayList<>();
    private ArrayAdapter<String> groupAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSendNotificationScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SendNotificationScreenArgs args = SendNotificationScreenArgs.fromBundle(getArguments());
        userName = args.getUserName();

        db = FirebaseFirestore.getInstance();

        setupAdapters();
        loadUserEvents();

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(SendNotificationScreen.this)
                        .navigate(SendNotificationScreenDirections.actionSendNotificationScreenToNotificationScreen(userName))
        );

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(SendNotificationScreen.this)
                        .navigate(SendNotificationScreenDirections.actionSendNotificationScreenToHomeScreen(userName))
        );

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(SendNotificationScreen.this)
                        .navigate(SendNotificationScreenDirections.actionSendNotificationScreenToProfileScreen(userName))
        );

        binding.btnSendMessage.setOnClickListener(v -> sendNotification());
    }

    private void setupAdapters() {
        eventAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, eventList);
        eventAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spEventSelect.setAdapter(eventAdapter);

        // Fixed group options
        groupList.add("waitList");
        groupList.add("selectedList");
        groupList.add("cancelledList");
        groupAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, groupList);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spGroupSelect.setAdapter(groupAdapter);
    }

    private void loadUserEvents() {
        db.collection("users")
                .document(userName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> organizedEventsMap = (Map<String, Object>) documentSnapshot.get("organizedEvents");
                        if (organizedEventsMap != null && organizedEventsMap.containsKey("events")) {
                            List<String> events = (List<String>) organizedEventsMap.get("events");
                            eventList.clear();
                            eventList.addAll(events);
                            eventAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(requireContext(), "No organized events found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "User not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to load user events", e);
                    Toast.makeText(requireContext(), "Error loading events.", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotification() {
        String selectedEvent = binding.spEventSelect.getSelectedItem() != null ?
                binding.spEventSelect.getSelectedItem().toString() : "";
        String selectedGroup = binding.spGroupSelect.getSelectedItem() != null ?
                binding.spGroupSelect.getSelectedItem().toString() : "";
        String messageText = binding.etMessageContent.getText().toString().trim();

        if (selectedEvent.isEmpty() || selectedGroup.isEmpty() || messageText.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill out all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("open events")
                .document(selectedEvent)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(requireContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Object groupObj = documentSnapshot.get(selectedGroup);
                    if (!(groupObj instanceof Map)) {
                        Toast.makeText(requireContext(), "Invalid group data format.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> groupMap = (Map<String, Object>) groupObj;
                    Object userListObj = groupMap.get("users");

                    if (!(userListObj instanceof List)) {
                        Toast.makeText(requireContext(), "No user list found in group.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> receivers = (List<String>) userListObj;
                    if (receivers.isEmpty()) {
                        Toast.makeText(requireContext(), "No users in this group.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (String receiver : receivers) {
                        Map<String, Object> notif = new HashMap<>();
                        notif.put("content", messageText);
                        notif.put("eventName", selectedEvent);
                        notif.put("receiver", receiver);
                        notif.put("sender", userName);
                        notif.put("timestamp", FieldValue.serverTimestamp());

                        db.collection("notification").add(notif)
                                .addOnSuccessListener(doc ->
                                        Log.d("Firestore", "✅ Notification sent to " + receiver))
                                .addOnFailureListener(e ->
                                        Log.e("Firestore", "❌ Failed to send notification", e));
                    }

                    Toast.makeText(requireContext(),
                            "Message sent to " + receivers.size() + " users.", Toast.LENGTH_SHORT).show();
                    binding.etMessageContent.setText("");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to load event data", e);
                    Toast.makeText(requireContext(), "Error sending notifications.", Toast.LENGTH_SHORT).show();
                });
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
