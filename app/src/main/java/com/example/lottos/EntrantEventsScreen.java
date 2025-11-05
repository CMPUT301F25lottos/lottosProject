package com.example.lottos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentEntrantEventsScreenBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntrantEventsScreen extends Fragment {

    private FragmentEntrantEventsScreenBinding binding;
    private FirebaseFirestore db;
    private ArrayList<String> openEvents;
    private ArrayAdapter<String> openEventsAdapter;
    private String userName;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentEntrantEventsScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get username from navigation arguments
        userName = EntrantEventsScreenArgs.fromBundle(getArguments()).getUserName();

        db = FirebaseFirestore.getInstance();
        openEvents = new ArrayList<>();

        // Setup adapter for ListView
        openEventsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, openEvents);
        binding.lvEntrantEvents.setAdapter(openEventsAdapter);

        // Back button → return to home screen
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(EntrantEventsScreen.this)
                        .navigate(EntrantEventsScreenDirections.actionEntrantEventsScreenToHomeScreen(userName))
        );

        // Display only open events
        displayOpenEvents();

        // When user taps on an event → confirm joining
        binding.lvEntrantEvents.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedEvent = openEvents.get(position);

            if (selectedEvent.equals("No events found.") || selectedEvent.equals("No open events available.")) {
                return;
            }

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Join Waitlist")
                    .setMessage("Do you want to join the waitlist for \"" + selectedEvent + "\"?")
                    .setPositiveButton("Yes", (dialog, which) -> addEntrantToWaitlist(userName, selectedEvent))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    /**
     * Loads and displays all open events where IsOpen == true
     */
    private void displayOpenEvents() {
        CollectionReference eventsRef = db.collection("open events");

        // Clear previous list
        openEvents.clear();

        eventsRef.get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot.isEmpty()) {
                openEvents.add("No events found.");
                openEventsAdapter.notifyDataSetChanged();
                return;
            }

            for (QueryDocumentSnapshot doc : querySnapshot) {
                Boolean isOpen = doc.getBoolean("IsOpen");
                String eventName = doc.getString("eventName");

                if (isOpen != null && isOpen && eventName != null) {
                    openEvents.add(eventName);
                }
            }

            if (openEvents.isEmpty()) {
                openEvents.add("No open events available.");
            }

            openEventsAdapter.notifyDataSetChanged();

        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to load open events.", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Adds an entrant to the waitlist for a given event — only if not already joined.
     */
    private void addEntrantToWaitlist(String userName, String eventName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference entrantDoc = db.collection("entrants").document(userName);
        DocumentReference eventDoc = db.collection("open events").document(eventName);

        // Step 1️⃣: Check if the entrant already joined this event
        entrantDoc.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Map<String, Object> waitListedMap = (Map<String, Object>) snapshot.get("waitListedEvents");
                if (waitListedMap != null) {
                    List<String> events = (List<String>) waitListedMap.get("events");
                    if (events != null && events.contains(eventName)) {
                        Toast.makeText(getContext(), "You’ve already in this waitlist.", Toast.LENGTH_SHORT).show();
                        return; // Stop here
                    }
                }
            }

            // Step 2️⃣: Proceed to add entrant to both places
            entrantDoc.update("waitListedEvents.events", FieldValue.arrayUnion(eventName))
                    .addOnSuccessListener(aVoid -> {
                        eventDoc.update("waitList.entrants.users", FieldValue.arrayUnion(userName))
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(getContext(),
                                            "You’ve joined the waitlist for " + eventName + "!",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(),
                                                "Failed to update event waitlist.",
                                                Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Failed to update your waitlisted events.",
                                    Toast.LENGTH_SHORT).show());
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(),
                        "Failed to check your current waitlists.",
                        Toast.LENGTH_SHORT).show()
        );
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
