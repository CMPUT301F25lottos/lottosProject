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

import com.example.lottos.databinding.FragmentEntrantWaitListsScreenBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays all events the entrant is waitlisted for.
 * Now shows event names instead of event IDs.
 */
public class EntrantWaitListsScreen extends Fragment {

    private FragmentEntrantWaitListsScreenBinding binding;
    private FirebaseFirestore db;

    // Display lists (names)
    private ArrayList<String> openWaitlistNames;
    private ArrayList<String> closedWaitlistNames;

    // Internal mapping so name → eventId for navigation
    private Map<String, String> nameToIdMap = new HashMap<>();

    private ArrayAdapter<String> openAdapter;
    private ArrayAdapter<String> closedAdapter;

    private String userName;

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

        userName = EntrantWaitListsScreenArgs.fromBundle(getArguments()).getUserName();
        db = FirebaseFirestore.getInstance();

        openWaitlistNames = new ArrayList<>();
        closedWaitlistNames = new ArrayList<>();

        setupButtons();
        setupAdapters();
        setupClickListeners();

        loadEntrantWaitlists();
    }

    private void setupButtons() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantWaitListsScreenDirections
                                .actionEntrantWaitListsScreenToHomeScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantWaitListsScreenDirections
                                .actionEntrantWaitListsScreenToNotificationScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantWaitListsScreenDirections
                                .actionEntrantWaitListsScreenToProfileScreen(userName)));
    }

    private void setupAdapters() {
        openAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, openWaitlistNames);

        closedAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, closedWaitlistNames);

        binding.lvOpenWaitLists.setAdapter(openAdapter);
        binding.lvClosedWaitLists.setAdapter(closedAdapter);
    }

    private void setupClickListeners() {
        binding.lvOpenWaitLists.setOnItemClickListener((parent, v, position, id) -> {
            String name = openWaitlistNames.get(position);
            if (!name.startsWith("No ")) {
                goToDetails(nameToIdMap.get(name));
            }
        });

        binding.lvClosedWaitLists.setOnItemClickListener((parent, v, position, id) -> {
            String name = closedWaitlistNames.get(position);
            if (!name.startsWith("No ")) {
                goToDetails(nameToIdMap.get(name));
            }
        });
    }

    /**
     * Load user's waitlist event IDs → fetch their names → categorize open/closed.
     */
    private void loadEntrantWaitlists() {

        DocumentReference userDoc = db.collection("users").document(userName);

        openWaitlistNames.clear();
        closedWaitlistNames.clear();
        nameToIdMap.clear();

        userDoc.get().addOnSuccessListener(snapshot -> {

            if (!snapshot.exists()) {
                Toast.makeText(getContext(), "User not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> waitMap =
                    (Map<String, Object>) snapshot.get("waitListedEvents");

            if (waitMap == null) {
                openWaitlistNames.add("No waitlisted events.");
                updateUI();
                return;
            }

            List<String> eventIds = (List<String>) waitMap.get("events");
            if (eventIds == null || eventIds.isEmpty()) {
                openWaitlistNames.add("No waitlisted events.");
                updateUI();
                return;
            }

            // Grab all open events
            db.collection("open events")
                    .get()
                    .addOnSuccessListener(query -> {

                        for (QueryDocumentSnapshot doc : query) {
                            String id = doc.getId();

                            // Only process events the user is in
                            if (!eventIds.contains(id)) continue;

                            String name = doc.getString("eventName");
                            Boolean openFlag = doc.getBoolean("IsOpen");
                            boolean isOpen = openFlag != null && openFlag;

                            if (name == null) name = "(Unnamed Event)";

                            nameToIdMap.put(name, id);

                            if (isOpen) {
                                openWaitlistNames.add(name);
                            } else {
                                closedWaitlistNames.add(name);
                            }
                        }

                        updateUI();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error loading events", e);
                        Toast.makeText(getContext(), "Error loading waitlists", Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to load user.", Toast.LENGTH_SHORT).show());
    }

    private void updateUI() {
        if (openWaitlistNames.isEmpty()) openWaitlistNames.add("No open waitlists.");
        if (closedWaitlistNames.isEmpty()) closedWaitlistNames.add("No closed waitlists.");

        openAdapter.notifyDataSetChanged();
        closedAdapter.notifyDataSetChanged();
    }

    private void goToDetails(String eventId) {
        if (eventId == null) return;
        NavHostFragment.findNavController(this)
                .navigate(EntrantWaitListsScreenDirections
                        .actionEntrantWaitListsScreenToEventDetailsScreen(userName, eventId));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
