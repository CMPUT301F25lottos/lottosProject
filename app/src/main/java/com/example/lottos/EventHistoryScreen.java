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

import com.example.lottos.account.ProfileScreen;
import com.example.lottos.account.ProfileScreenDirections;
import com.example.lottos.databinding.FragmentEventHistoryScreenBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * this fragment displays the entrant user's event history.
 * Role: Reads historical event data from Firestore
 * (based on the user's stored lists) and shows it in a simple list with navigation
 * back to the home screen.
 */
public class EventHistoryScreen extends Fragment {

    private FragmentEventHistoryScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;

    // List of *event names* to display
    private final List<String> eventHistoryNames = new ArrayList<>();
    private ArrayAdapter<String> historyAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventHistoryScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = EventHistoryScreenArgs.fromBundle(getArguments()).getUserName();
        db = FirebaseFirestore.getInstance();

        // Set up adapter once; we'll just update the backing list
        historyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                eventHistoryNames
        );
        binding.lvEventHistory.setAdapter(historyAdapter);

        loadHistory();
        setupNavButtons();


    }

    private void loadHistory() {
        db.collection("users").document(userName).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        Toast.makeText(getContext(), "User not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Firestore structure: waitListedEvents (map) -> events (array of eventIds)
                    List<String> historyIds = (List<String>) snapshot.get("waitListedEvents.events");

                    if (historyIds == null || historyIds.isEmpty()) {
                        Toast.makeText(getContext(), "No past events found.", Toast.LENGTH_SHORT).show();
                        eventHistoryNames.clear();
                        historyAdapter.notifyDataSetChanged();
                        return;
                    }

                    // Clear and resolve each eventId to its eventName
                    eventHistoryNames.clear();
                    historyAdapter.notifyDataSetChanged();

                    for (String eventId : historyIds) {
                        resolveEventName(eventId, eventName -> {
                            eventHistoryNames.add(eventName);
                            historyAdapter.notifyDataSetChanged();
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading history", Toast.LENGTH_SHORT).show());
    }

    /**
     * Helper to resolve an eventId to its eventName from /open events/{eventId}.
     * Falls back to the eventId if name is missing or lookup fails.
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
                        name = eventId;
                    }
                    callback.onNameResolved(name);
                })
                .addOnFailureListener(e -> callback.onNameResolved(eventId));
    }

    private interface EventNameCallback {
        void onNameResolved(String eventName);
    }

    private void setupNavButtons() {

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToHomeScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToNotificationScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToProfileScreen(userName)));

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToOrganizerEventsScreen(userName))
        );

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
