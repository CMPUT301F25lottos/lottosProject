package com.example.lottos.events;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lottos.EventListAdapter;
import com.example.lottos.databinding.FragmentEntrantEventsScreenBinding;

import java.util.ArrayList;
import java.util.List;

public class EntrantEventsScreen extends Fragment {

    private FragmentEntrantEventsScreenBinding binding;
    private EntrantEventManager manager;

    private String userName;

    // Adapter data
    private final List<EventListAdapter.EventItem> eventItems = new ArrayList<>();
    private EventListAdapter adapter;
    private List<String> userWaitlistedEvents = new ArrayList<>();

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
        manager = new EntrantEventManager();

        setupRecycler();
        setupNavButtons();

        loadEvents();
    }

    // -----------------------------------------------------------
    // RECYCLER VIEW
    // -----------------------------------------------------------
    private void setupRecycler() {
        adapter = new EventListAdapter(
                eventItems,
                true,     // show join/leave button for entrant
                new EventListAdapter.Listener() {

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
                }
        );

        binding.rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEvents.setAdapter(adapter);
    }

    // -----------------------------------------------------------
    // LOAD DATA
    // -----------------------------------------------------------
    private void loadEvents() {

        manager.loadEventsForUser(userName, new EntrantEventManager.EventsCallback() {
            @Override
            public void onSuccess(List<EntrantEventManager.EventModel> list,
                                  List<String> waitlisted) {

                userWaitlistedEvents = waitlisted;

                eventItems.clear();

                for (EntrantEventManager.EventModel evt : list) {
                    boolean isJoined = waitlisted.contains(evt.id);

                    eventItems.add(
                            new EventListAdapter.EventItem(
                                    evt.id,
                                    evt.name,
                                    evt.isOpen,
                                    isJoined
                            )
                    );
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -----------------------------------------------------------
    // ACTIONS
    // -----------------------------------------------------------
    private void join(String eventId) {
        manager.joinWaitlist(
                userName,
                eventId,
                () -> {
                    Toast.makeText(getContext(), "Joined waitlist", Toast.LENGTH_SHORT).show();
                    loadEvents();   // refresh states
                },
                e -> Toast
                        .makeText(getContext(), "Join failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
        );
    }

    private void leave(String eventId) {
        manager.leaveWaitlist(
                userName,
                eventId,
                () -> {
                    Toast.makeText(getContext(), "Left waitlist", Toast.LENGTH_SHORT).show();
                    loadEvents();
                },
                e -> Toast
                        .makeText(getContext(), "Leave failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
        );
    }

    // -----------------------------------------------------------
    // NAVIGATION
    // -----------------------------------------------------------
    private void goToDetails(String eventId) {
        NavHostFragment.findNavController(this)
                .navigate(EntrantEventsScreenDirections
                        .actionEntrantEventsScreenToEventDetailsScreen(userName, eventId));
    }

    private void setupNavButtons() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantEventsScreenDirections
                                .actionEntrantEventsScreenToHomeScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantEventsScreenDirections
                                .actionEntrantEventsScreenToNotificationScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantEventsScreenDirections
                                .actionEntrantEventsScreenToProfileScreen(userName)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
