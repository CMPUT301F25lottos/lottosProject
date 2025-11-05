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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}