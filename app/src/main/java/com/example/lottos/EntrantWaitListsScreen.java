package com.example.lottos;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentEntrantWaitListsScreenBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntrantWaitListsScreen extends Fragment {

    private FragmentEntrantWaitListsScreenBinding binding;
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private ArrayList<String> openWaitlists;
    private ArrayList<String> closedWaitlists;
    private ArrayAdapter<String> openAdapter;
    private ArrayAdapter<String> closedAdapter;

    private String userName;
    private String selectedEvent = null;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentEntrantWaitListsScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get username passed from navigation args
        userName = EntrantWaitListsScreenArgs.fromBundle(getArguments()).getUserName();

        // Setup Firestore and lists
        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("open events");
        openWaitlists = new ArrayList<>();
        closedWaitlists = new ArrayList<>();

        // Back button → home screen
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(EntrantWaitListsScreen.this)
                        .navigate(EntrantWaitListsScreenDirections.actionEntrantWaitListsScreenToHomeScreen(userName))
        );

        // Initialize adapters once
        openAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, openWaitlists);
        closedAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, closedWaitlists);
        binding.lvOpenWaitLists.setAdapter(openAdapter);
        binding.lvClosedWaitLists.setAdapter(closedAdapter);

        // Tap to select an open event
        binding.lvOpenWaitLists.setOnItemClickListener((parent, v1, position, id) -> {
            selectedEvent = openWaitlists.get(position);
            Toast.makeText(getContext(), "Selected: " + selectedEvent, Toast.LENGTH_SHORT).show();
        });

        // Delete button click → show confirmation
        binding.btnDeleteEvent.setOnClickListener(v -> {
            if (selectedEvent != null) {
                showDeleteConfirmation(selectedEvent);
            } else {
                Toast.makeText(getContext(), "No event selected", Toast.LENGTH_SHORT).show();
            }
        });

        // Tap to select a closed event → event can't be modified
        binding.lvClosedWaitLists.setOnItemClickListener((parent, v1, position, id) -> {
            selectedEvent = closedWaitlists.get(position);
            Toast.makeText(getContext(),  selectedEvent + "can't be modified", Toast.LENGTH_SHORT).show();
        });

        // Load events from Firestore
        loadEntrantWaitlists(userName);
    }

    /** Load user's waitlisted events and categorize open/closed */
    private void loadEntrantWaitlists(String userName) {
        DocumentReference usersDoc = db.collection("users").document(userName);
        openWaitlists.clear();
        closedWaitlists.clear();

        usersDoc.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(getContext(), "users not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> waitListedMap = (Map<String, Object>) snapshot.get("waitListedEvents");
            if (waitListedMap == null) return;

            List<String> waitListEvents = (List<String>) waitListedMap.get("events");
            if (waitListEvents == null || waitListEvents.isEmpty()) {
                openWaitlists.add("No waitlisted events found.");
                updateUI();
                return;
            }

            final int total = waitListEvents.size();
            final int[] counter = {0};

            for (String eventName : waitListEvents) {
                eventsRef.whereEqualTo("eventName", eventName).get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                for (DocumentSnapshot eventSnapshot : querySnapshot) {
                                    boolean isOpen = Boolean.TRUE.equals(eventSnapshot.getBoolean("IsOpen"));
                                    if (isOpen) openWaitlists.add(eventName);
                                    else closedWaitlists.add(eventName);
                                }
                            } else {
                                closedWaitlists.add(eventName + " (unavailable)");
                            }

                            counter[0]++;
                            if (counter[0] == total) updateUI();
                        })
                        .addOnFailureListener(e -> {
                            counter[0]++;
                            if (counter[0] == total) updateUI();
                        });
            }

        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to load waitlists.", Toast.LENGTH_SHORT).show()
        );
    }

    /** Show confirmation popup before quitting event */
    private void showDeleteConfirmation(String eventName) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Quit Event")
                .setMessage("Are you sure you want to quit \"" + eventName + "\"?")
                .setPositiveButton("Yes", (dialog, which) -> deleteEntrantFromWaitlist(userName, eventName))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Remove entrant from event’s waitlist in Firestore */
    private void deleteEntrantFromWaitlist(String userName, String eventName) {
        DocumentReference usersDoc = db.collection("users").document(userName);
        usersDoc.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Map<String, Object> waitListedMap = (Map<String, Object>) snapshot.get("waitListedEvents");
                if (waitListedMap != null) {
                    List<String> events = (List<String>) waitListedMap.get("events");
                    if (events != null && events.contains(eventName)) {
                        events.remove(eventName);
                        usersDoc.update("waitListedEvents.events", events)
                                .addOnSuccessListener(aVoid -> {
                                    openWaitlists.remove(eventName);
                                    openAdapter.notifyDataSetChanged();
                                    Toast.makeText(getContext(), "You quit " + eventName, Toast.LENGTH_SHORT).show();
                                    selectedEvent = null;
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Failed to update Firestore.", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    /** Refresh ListView contents */
    private void updateUI() {
        if (openWaitlists.isEmpty()) openWaitlists.add("No joined open waitlists.");
        if (closedWaitlists.isEmpty()) closedWaitlists.add("No joined closed waitlists.");
        openAdapter.notifyDataSetChanged();
        closedAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
