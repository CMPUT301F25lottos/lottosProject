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
import com.example.lottos.organizer.OrganizerEventsScreenArgs;
import com.example.lottos.organizer.OrganizerEventsScreenDirections;
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

    // Use a simple list of strings for the ArrayAdapter and a corresponding list for event IDs.
    private final List<String> eventNames = new ArrayList<>();
    private final List<String> eventIds = new ArrayList<>();
    private ArrayAdapter<String> adapter;

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

        // 1. Setup ListView and ArrayAdapter
        ListView listView = binding.lvOpenEvents;
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, eventNames);
        listView.setAdapter(adapter);

        // 2. Set a click listener to handle item clicks
        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            String clickedEventId = eventIds.get(position); // Get the ID using the item's position
            openEditEventScreen(clickedEventId);
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
                    // Clear previous data
                    eventNames.clear();
                    eventIds.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String id = doc.getId();
                        String name = doc.getString("eventName");

                        if (name != null) {
                            eventNames.add(name); // Add the name to the list for the adapter
                            eventIds.add(id);     // Add the ID to our parallel list
                        }
                    }

                    // Notify the adapter that the data has changed
                    adapter.notifyDataSetChanged();

                    if (eventNames.isEmpty()) {
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
