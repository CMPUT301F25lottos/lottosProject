package com.example.lottos.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.EventRepository;
import com.example.lottos.databinding.FragmentOrganizerEventsScreenBinding;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEventsScreen extends Fragment {

    private FragmentOrganizerEventsScreenBinding binding;
    private EventRepository repo;
    private String userName;

    private final List<String> eventNames = new ArrayList<>();
    private final List<String> eventIds = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    // stores the currently selected event
    private String selectedEventId = null;

    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             android.view.ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrganizerEventsScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = OrganizerEventsScreenArgs.fromBundle(getArguments()).getUserName();
        repo = new EventRepository();

        ListView listView = binding.lvOpenEvents;
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, eventNames);
        listView.setAdapter(adapter);

        // enable single-item selection
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            selectedEventId = eventIds.get(position);
            listView.setItemChecked(position, true);

            Toast.makeText(getContext(),
                    "Selected: " + eventNames.get(position),
                    Toast.LENGTH_SHORT
            ).show();
        });

        loadOrganizerEvents();
        setupNavButtons();
    }

    private void loadOrganizerEvents() {
        repo.getEventsByOrganizer(userName).get()
                .addOnSuccessListener(query -> {
                    eventNames.clear();
                    eventIds.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String id = doc.getId();
                        String name = doc.getString("eventName");

                        if (name != null) {
                            eventNames.add(name);
                            eventIds.add(id);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (eventNames.isEmpty()) {
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

    private void openEditEventScreen(String eventId) {
        NavHostFragment.findNavController(this)
                .navigate(OrganizerEventsScreenDirections
                        .actionOrganizerEventsScreenToEditEventScreen(userName, eventId));
    }

    // 🔹 NEW: Organizer-specific details screen
    private void openOrganizerEventDetailsScreen(String eventId) {
        NavHostFragment.findNavController(this)
                .navigate(OrganizerEventsScreenDirections
                        .actionOrganizerEventsScreenToOrganizerEventDetailsScreen(userName, eventId));
    }

    private void setupNavButtons() {

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

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventsScreenDirections
                                .actionOrganizerEventsScreenToEventHistoryScreen(userName))
        );

        // EDIT EVENT
        binding.btnEditEvent.setOnClickListener(v -> {
            if (selectedEventId == null) {
                Toast.makeText(getContext(),
                        "Please select an event first.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            openEditEventScreen(selectedEventId);
        });

        // VIEW DETAILS → Organizer-specific screen
        binding.btnViewEventDetails.setOnClickListener(v -> {
            if (selectedEventId == null) {
                Toast.makeText(getContext(),
                        "Please select an event first.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            openOrganizerEventDetailsScreen(selectedEventId);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
