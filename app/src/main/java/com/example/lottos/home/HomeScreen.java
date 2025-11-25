package com.example.lottos.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lottos.EventListAdapter;
import com.example.lottos.events.EntrantEventManager;
import com.example.lottos.databinding.FragmentHomeScreenBinding;
import com.example.lottos.events.EntrantEventsScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Home Screen Fragment.
 * - Updates event statuses
 * - Shows list of events (like old EntrantEventsScreen)
 * - Handles navigation buttons
 */

public class HomeScreen extends Fragment {

    private FragmentHomeScreenBinding binding;
    private EventStatusUpdater eventUpdater;
    private EntrantEventManager manager;

    private String userName;

    // Recycler / adapter data
    private final List<EventListAdapter.EventItem> eventItems = new ArrayList<>();
    private EventListAdapter adapter;
    private List<String> userWaitlistedEvents = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = HomeScreenArgs.fromBundle(getArguments()).getUserName();

        eventUpdater = new EventStatusUpdater();
        manager = new EntrantEventManager();

        // 1️⃣ Update event statuses first
        eventUpdater.updateEventStatuses(new EventStatusUpdater.UpdateListener() {
            @Override
            public void onUpdateSuccess(int updatedCount) {
                if (updatedCount > 0) {
                    Toast.makeText(getContext(),
                            "Updated " + updatedCount + " event statuses.",
                            Toast.LENGTH_SHORT).show();
                }
                // After updating statuses, load the events into the list
                loadEvents();
            }

            @Override
            public void onUpdateFailure(String errorMessage) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                // Even if update fails, we can still try to load events
                loadEvents();
            }
        });

        // 2️⃣ Setup RecyclerView
        setupRecycler();

        // 3️⃣ Setup navigation buttons
        setupNavButtons();
    }

    // -----------------------------------------------------------
    // RECYCLER VIEW
    // -----------------------------------------------------------
    private void setupRecycler() {
        adapter = new EventListAdapter(
                eventItems,
                eventId -> goToDetails(eventId)   // click → go to details
        );

        binding.rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEvents.setAdapter(adapter);
    }

    // -----------------------------------------------------------
    // LOAD DATA (from EntrantEventManager)
    // -----------------------------------------------------------
    private void loadEvents() {
        manager.loadEventsForUser(userName, new EntrantEventManager.EventsCallback() {
            @Override
            public void onSuccess(List<EntrantEventManager.EventModel> list,
                                  List<String> waitlisted) {

                userWaitlistedEvents = waitlisted;
                eventItems.clear();

                for (EntrantEventManager.EventModel evt : list) {
                    eventItems.add(
                            new EventListAdapter.EventItem(
                                    evt.id,
                                    evt.name,
                                    evt.isOpen,
                                    evt.location,
                                    evt.startTime,
                                    evt.endTime
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
    // NAVIGATION
    // -----------------------------------------------------------
    private void goToDetails(String eventId) {
        NavHostFragment.findNavController(this)
                .navigate(HomeScreenDirections
                        .actionHomeScreenToEventDetailsScreen(userName, eventId));
    }

    private void setupNavButtons() {
        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections.actionHomeScreenToProfileScreen(userName)));

        binding.btnInfo.setOnClickListener(v ->
                 NavHostFragment.findNavController(HomeScreen.this)
                         .navigate(HomeScreenDirections.actionHomeScreenToLotteryInfoScreen(userName)));

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections.actionHomeScreenToOrganizerEventsScreen(userName)));

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections.actionHomeScreenToEventHistoryScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections.actionHomeScreenToNotificationScreen(userName)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
