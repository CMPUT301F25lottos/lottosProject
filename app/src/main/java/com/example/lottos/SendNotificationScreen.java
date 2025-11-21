package com.example.lottos;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendNotificationScreen extends Fragment {

    private FragmentSendNotificationScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;

    private final List<String> eventNames = new ArrayList<>();
    private final List<String> eventIds = new ArrayList<>();

    private ArrayAdapter<String> eventAdapter;

    private final List<String> groups = List.of("waitList", "selectedList", "cancelledList");
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

        userName = SendNotificationScreenArgs.fromBundle(getArguments()).getUserName();
        db = FirebaseFirestore.getInstance();

        setupAdapters();
        loadOrganizerEvents();

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

        binding.btnSendMessage.setOnClickListener(v -> sendNotification());
    }

    // ----------------------------------------------------------
    // ADAPTER SETUP
    // ----------------------------------------------------------
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

    // ----------------------------------------------------------
    // LOAD ORGANIZER'S EVENTS (ID + name)
    // ----------------------------------------------------------
    private void loadOrganizerEvents() {
        db.collection("open events")
                .whereEqualTo("organizer", userName)
                .get()
                .addOnSuccessListener(query -> {

                    eventIds.clear();
                    eventNames.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("eventName");

                        if (name == null) name = "(Unnamed Event)";

                        eventIds.add(id);
                        eventNames.add(name);
                    }

                    if (eventIds.isEmpty()) {
                        Toast.makeText(requireContext(), "You have no events.", Toast.LENGTH_SHORT).show();
                    }

                    eventAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed loading events", e);
                    Toast.makeText(requireContext(), "Error loading events.", Toast.LENGTH_SHORT).show();
                });
    }

    // ----------------------------------------------------------
    // SEND NOTIFICATIONS
    // ----------------------------------------------------------
    private void sendNotification() {

        if (eventIds.isEmpty()) {
            Toast.makeText(requireContext(), "You have no events to send notifications for.", Toast.LENGTH_SHORT).show();
            return;
        }

        int eventIndex = binding.spEventSelect.getSelectedItemPosition();
        String group = binding.spGroupSelect.getSelectedItem().toString();
        String message = binding.etMessageContent.getText().toString().trim();

        if (eventIndex < 0 || message.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        String eventId = eventIds.get(eventIndex);
        String eventName = eventNames.get(eventIndex);

        db.collection("open events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {

                    Map<String, Object> groupData = (Map<String, Object>) doc.get(group);

                    if (groupData == null) {
                        Toast.makeText(requireContext(), "Group is empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> users = (List<String>) groupData.get("users");

                    if (users == null || users.isEmpty()) {
                        Toast.makeText(requireContext(), "No users in this group.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (String receiver : users) {

                        Map<String, Object> notif = new HashMap<>();
                        notif.put("content", message);
                        notif.put("eventName", eventName); // <<< FIXED (name, not ID)
                        notif.put("receiver", receiver);
                        notif.put("sender", userName);
                        notif.put("timestamp", FieldValue.serverTimestamp());

                        db.collection("notification")
                                .add(notif)
                                .addOnSuccessListener(x ->
                                        Log.d("Firestore", "Sent to " + receiver)
                                )
                                .addOnFailureListener(e ->
                                        Log.e("Firestore", "Failed", e)
                                );
                    }

                    Toast.makeText(requireContext(),
                            "Message sent to " + users.size() + " users.",
                            Toast.LENGTH_SHORT
                    ).show();

                    binding.etMessageContent.setText("");

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error sending.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
