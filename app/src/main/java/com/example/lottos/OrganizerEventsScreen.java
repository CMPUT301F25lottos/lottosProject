package com.example.lottos;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentOrganizerEventsScreenBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * This fragment displays all events created by a specific organizer.
 */
public class OrganizerEventsScreen extends Fragment {

    private FragmentOrganizerEventsScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;
    private final List<String> organizerEvents = new ArrayList<>();
    private ArrayAdapter<String> eventAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrganizerEventsScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = OrganizerEventsScreenArgs.fromBundle(getArguments()).getUserName();
        db = FirebaseFirestore.getInstance();

        // ListView setup
        ListView listView = binding.lvOpenEvents;

        eventAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                organizerEvents
        );
        listView.setAdapter(eventAdapter);

        // Click handler → go to EditEvent screen
        listView.setOnItemClickListener((parent, clickedView, position, id) -> {
            String eventName = organizerEvents.get(position);
            openEditEventScreen(userName, eventName);
        });

        // Navigation buttons
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventsScreenDirections
                                .actionOrganizerEventsScreenToHomeScreen(userName))
        );

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventsScreenDirections
                                .actionOrganizerEventsScreenToNotificationScreen(userName))
        );

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventsScreenDirections
                                .actionOrganizerEventsScreenToProfileScreen(userName))
        );

        binding.btnCreateEvent.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventsScreenDirections
                                .actionOrganizerEventsScreenToCreateEventScreen(userName))
        );

        // Load events
        loadOrganizerEvents();
    }

    /** Load all events created by this organizer from Firestore */
    private void loadOrganizerEvents() {
        db.collection("open events")
                .whereEqualTo("organizer", userName)
                .get()
                .addOnSuccessListener(query -> {
                    organizerEvents.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String eventName = doc.getString("eventName");
                        if (eventName != null) {
                            organizerEvents.add(eventName);
                        }
                    }

                    eventAdapter.notifyDataSetChanged();

                    if (organizerEvents.isEmpty()) {
                        Toast.makeText(getContext(),
                                "You haven’t created any events yet.",
                                Toast.LENGTH_SHORT).show();
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to load events", e);
                    Toast.makeText(getContext(),
                            "Error loading events.", Toast.LENGTH_SHORT).show();
                });
    }

    /** Navigate to EditEventScreen */
    private void openEditEventScreen(String userName, String eventName) {
        OrganizerEventsScreenDirections.ActionOrganizerEventsScreenToEditEventScreen action =
                OrganizerEventsScreenDirections.actionOrganizerEventsScreenToEditEventScreen(userName, eventName);
        NavHostFragment.findNavController(this).navigate(action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
