// OrganizerEventsScreen.java (refactored)
package com.example.lottos;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lottos.databinding.FragmentOrganizerEventsScreenBinding;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * This fragment displays all events created by a specific organizer.
 */
public class OrganizerEventsScreen extends Fragment {

    private FragmentOrganizerEventsScreenBinding binding;
    private EventRepository repo;
    private String userName;

    private final List<EventListAdapter.EventItem> events = new ArrayList<>();
    private EventListAdapter adapter;

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

        adapter = new EventListAdapter(events, false, new EventListAdapter.Listener() {

            @Override
            public void onEventClick(String eventId) {
                openEditEventScreen(eventId);
            }

            @Override public void onJoinClick(String eventId) {}
            @Override public void onLeaveClick(String eventId) {}

        });

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
        repo.getEventsByOrganizer(userName).get()
                .addOnSuccessListener(query -> {
                    events.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String id = doc.getId();
                        String name = doc.getString("eventName");

                        if (name != null) {
                            events.add(new EventListAdapter.EventItem(
                                    id, name, true, false
                            ));
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (events.isEmpty()) {
                        Toast.makeText(getContext(), "You haven’t created any events yet.",
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
