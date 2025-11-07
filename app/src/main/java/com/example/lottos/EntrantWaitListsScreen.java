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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntrantWaitListsScreen extends Fragment {

    private FragmentEntrantWaitListsScreenBinding binding;
    private FirebaseFirestore db;
    private ArrayList<String> openWaitlists;
    private ArrayList<String> closedWaitlists;
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

        // Get username passed from navigation args
        userName = EntrantWaitListsScreenArgs.fromBundle(getArguments()).getUserName();

        db = FirebaseFirestore.getInstance();
        openWaitlists = new ArrayList<>();
        closedWaitlists = new ArrayList<>();

        // Back button → home screen
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(EntrantWaitListsScreen.this)
                        .navigate(EntrantWaitListsScreenDirections
                                .actionEntrantWaitListsScreenToHomeScreen(userName))
        );

        // Initialize adapters
        openAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, openWaitlists);
        closedAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, closedWaitlists);
        binding.lvOpenWaitLists.setAdapter(openAdapter);
        binding.lvClosedWaitLists.setAdapter(closedAdapter);

        // Handle clicks on open/closed events
        binding.lvOpenWaitLists.setOnItemClickListener((parent, v1, position, id) -> {
            String eventId = openWaitlists.get(position);
            if (!eventId.startsWith("No ")) {
                goToDetails(eventId);
            }
        });

        binding.lvClosedWaitLists.setOnItemClickListener((parent, v2, position, id) -> {
            String eventId = closedWaitlists.get(position);
            if (!eventId.startsWith("No ")) {
                goToDetails(eventId);
            }
        });

        loadEntrantWaitlists();
    }

    /** Load user's waitlisted events and categorize open/closed (using document IDs like EntrantEventsScreen) */
    private void loadEntrantWaitlists() {
        DocumentReference userDoc = db.collection("users").document(userName);
        openWaitlists.clear();
        closedWaitlists.clear();

        userDoc.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(getContext(), "User not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> waitListedMap = (Map<String, Object>) snapshot.get("waitListedEvents");
            if (waitListedMap == null) {
                openWaitlists.add("No waitlisted events found.");
                updateUI();
                return;
            }

            List<String> waitListEvents = (List<String>) waitListedMap.get("events");
            if (waitListEvents == null || waitListEvents.isEmpty()) {
                openWaitlists.add("No waitlisted events found.");
                updateUI();
                return;
            }

            // Now mimic EntrantEventsScreen: read from "open events" and use document IDs
            db.collection("open events").get().addOnSuccessListener(query -> {
                for (QueryDocumentSnapshot doc : query) {
                    String docId = doc.getId();
                    Boolean isOpenFlag = doc.getBoolean("IsOpen");
                    boolean isOpen = isOpenFlag != null && isOpenFlag;

                    if (waitListEvents.contains(docId)) {
                        if (isOpen) openWaitlists.add(docId);
                        else closedWaitlists.add(docId);
                    }
                }

                // Sort to match EntrantEventsScreen order (open first)
                openWaitlists.sort(String::compareToIgnoreCase);
                closedWaitlists.sort(String::compareToIgnoreCase);

                updateUI();
            }).addOnFailureListener(e -> {
                Log.e("Firestore", "Failed to load events", e);
                Toast.makeText(getContext(), "Error loading waitlists", Toast.LENGTH_SHORT).show();
            });

        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show()
        );
    }

    /** Go to Event Details (exact same navigation style as EntrantEventsScreen) */
    private void goToDetails(String eventId) {
        EntrantWaitListsScreenDirections.ActionEntrantWaitListsScreenToEventDetailsScreen action =
                EntrantWaitListsScreenDirections
                        .actionEntrantWaitListsScreenToEventDetailsScreen(userName, eventId);
        NavHostFragment.findNavController(this).navigate(action);
    }

    /** Update list views */
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
