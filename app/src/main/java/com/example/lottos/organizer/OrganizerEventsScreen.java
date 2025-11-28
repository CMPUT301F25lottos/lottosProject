package com.example.lottos.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottos.EventListAdapter;
import com.example.lottos.EventRepository;
import com.example.lottos.databinding.FragmentOrganizerEventsScreenBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEventsScreen extends Fragment {
    private FragmentOrganizerEventsScreenBinding binding;
    private EventRepository repo;
    private String userName;
    private final List<EventListAdapter.EventItem> events = new ArrayList<>();
    private EventListAdapter adapter;
    private String selectedEventId = null;

    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrganizerEventsScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = OrganizerEventsScreenArgs.fromBundle(getArguments()).getUserName();
        repo = new EventRepository();

        RecyclerView rv = binding.rvOrganizerEvents;
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new EventListAdapter(events, new EventListAdapter.Listener() {
            @Override
            public void onEventClick(String eventId) {
                openOrganizerEventDetailsScreen(eventId);
            }
            @Override
            public void onEventSelected(String eventId) {
                selectedEventId = eventId;
                Toast.makeText(getContext(),
                        "Selected event", Toast.LENGTH_SHORT).show();
            }
        });

        rv.setAdapter(adapter);
        loadOrganizerEvents();
        setupNavButtons();
    }

    private void loadOrganizerEvents() {
        repo.getEventsByOrganizer(userName).get()
                .addOnSuccessListener(query -> {
                    events.clear();

                    for (QueryDocumentSnapshot doc : query) {

                        String id = doc.getId();
                        String name = doc.getString("eventName");
                        String location = doc.getString("location");

                        Timestamp startTs = doc.getTimestamp("startTime");
                        Timestamp endTs   = doc.getTimestamp("endTime");

                        String startTimeText = startTs != null ? formatTimestamp(startTs) : "";
                        String endTimeText   = endTs != null ? formatTimestamp(endTs) : "";

                        String posterUrl = doc.getString("posterUrl");

                        if (name != null) {
                            events.add(new EventListAdapter.EventItem(
                                    id,
                                    name,
                                    true,
                                    location,
                                    startTimeText,
                                    endTimeText,
                                    posterUrl
                            ));
                        }
                    }
                    adapter.notifyDataSetChanged();

                    if (events.isEmpty()) {
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

    private void openOrganizerEventDetailsScreen(String eventId) {
        NavHostFragment.findNavController(this)
                .navigate(OrganizerEventsScreenDirections
                        .actionOrganizerEventsScreenToOrganizerEventDetailsScreen(userName, eventId));
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "";
        java.util.Date date = ts.toDate();
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault());
        return sdf.format(date);
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

        binding.btnEditEvent.setOnClickListener(v -> {
            if (selectedEventId == null) {
                Toast.makeText(getContext(),
                        "Please select an event first.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            openEditEventScreen(selectedEventId);
        });

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
