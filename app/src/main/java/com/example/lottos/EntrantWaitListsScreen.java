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
