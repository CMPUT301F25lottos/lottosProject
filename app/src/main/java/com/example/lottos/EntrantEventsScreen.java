// EntrantEventsScreen.java (refactored)
package com.example.lottos;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lottos.databinding.FragmentEntrantEventsScreenBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntrantEventsScreen extends Fragment {

    private FragmentEntrantEventsScreenBinding binding;
    private EventRepository repo;
    private EntrantEventManager manager;
    private FirebaseFirestore db;

    private final List<EventListAdapter.EventItem> events = new ArrayList<>();
    private List<String> userWaitlistedEvents = new ArrayList<>();

    private String userName;
    private EventListAdapter adapter;

    private EventListAdapter adapter;

    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             android.view.ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEntrantEventsScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = EntrantEventsScreenArgs.fromBundle(getArguments()).getUserName();

        repo = new EventRepository();
        manager = new EntrantEventManager();
        userName = EntrantEventsScreenArgs.fromBundle(getArguments()).getUserName();
        db = FirebaseFirestore.getInstance();

        adapter = new EventListAdapter(events, true, new EventListAdapter.Listener() {

            @Override
            public void onEventClick(String eventId) {
                goToDetails(eventId);
            }

            @Override
            public void onJoinClick(String eventId) {
                join(eventId);
            }

            @Override
            public void onLeaveClick(String eventId) {
                leave(eventId);
            }
        });

        binding.recyclerEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerEvents.setAdapter(adapter);
        adapter = new EventListAdapter();
        binding.lvEvents.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantEventsScreenDirections.actionEntrantEventsScreenToHomeScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantEventsScreenDirections.actionEntrantEventsScreenToNotificationScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantEventsScreenDirections.actionEntrantEventsScreenToProfileScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantEventsScreenDirections.actionEntrantEventsScreenToNotificationScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantEventsScreenDirections.actionEntrantEventsScreenToProfileScreen(userName)));

        loadUserWaitlistedEvents();
    }

    private void loadUserWaitlistedEvents() {
        db.collection("userss").document(userName).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        Map<String, Object> w = (Map<String, Object>) snap.get("waitListedEvents");
                        if (w != null && w.containsKey("events")) {
                            userWaitlistedEvents = (List<String>) w.get("events");
                        }
                    }
                    loadEvents();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to load waitlist", e);
                    loadEvents();
                });
    }

    /** Load all open events */
    private void loadEvents() {
        repo.getAllEvents().get()
                .addOnSuccessListener(query -> {
                    events.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String id = doc.getId();
                        String name = doc.getString("eventName");
                        Boolean openFlag = doc.getBoolean("IsOpen");

                        if (name != null) {
                            events.add(new EventListAdapter.EventItem(
                                    id,
                                    name,
                                    openFlag != null && openFlag,
                                    userWaitlistedEvents.contains(id)
                            ));
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to load events", e);
                    Toast.makeText(getContext(), "Error loading events.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void join(String eventId) {
        manager.joinWaitlist(userName, eventId,
                () -> {
                    for (EventListAdapter.EventItem evt : events) {
                        if (evt.id.equals(eventId)) evt.isJoined = true;
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Joined waitlist", Toast.LENGTH_SHORT).show();
                },
                e -> Toast.makeText(getContext(), "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void leave(String eventId) {
        manager.leaveWaitlist(userName, eventId,
                () -> {
                    for (EventListAdapter.EventItem evt : events) {
                        if (evt.id.equals(eventId)) evt.isJoined = false;
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Left waitlist", Toast.LENGTH_SHORT).show();
                },
                e -> Toast.makeText(getContext(), "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void goToDetails(String eventId) {
        NavHostFragment.findNavController(this)
                .navigate(EntrantEventsScreenDirections
                        .actionEntrantEventsScreenToEventDetailsScreen(userName, eventId));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
