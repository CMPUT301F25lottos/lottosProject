package com.example.lottos.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lottos.EventListAdapter;
import com.example.lottos.databinding.FragmentHomeScreenBinding;
import com.example.lottos.events.EntrantEventManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Home Screen Fragment.
 * - Checks if user is Admin or regular User.
 * - Updates event statuses.
 * - Shows list of events based on user role.
 * - Handles navigation buttons.
 */
public class HomeScreen extends Fragment {

    private FragmentHomeScreenBinding binding;
    private EventStatusUpdater eventUpdater;
    private EntrantEventManager manager;

    private String userName;
    private boolean isAdmin = false; // Flag for user role

    // Recycler / adapter data
    private final List<EventListAdapter.EventItem> eventItems = new ArrayList<>();
    private EventListAdapter adapter;

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

        // 1. CHECK THE USER'S ROLE
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);

        eventUpdater = new EventStatusUpdater();
        manager = new EntrantEventManager();

        // 2. Update event statuses first
        eventUpdater.updateEventStatuses(new EventStatusUpdater.UpdateListener() {
            @Override
            public void onUpdateSuccess(int updatedCount) {
                // After updating statuses, load the events based on user role
                loadEventsBasedOnRole();
            }

            @Override
            public void onUpdateFailure(String errorMessage) {
                Toast.makeText(getContext(), "Status update failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                // Even if update fails, still try to load events
                loadEventsBasedOnRole();
            }
        });

        setupRecycler();
        setupNavButtons();
    }

    private void setupRecycler() {
        // In HomeScreen, clicking an event directly navigates to its details
        adapter = new EventListAdapter(
                eventItems,
                new EventListAdapter.Listener() {
                    @Override
                    public void onEventClick(String eventId) {
                        // Navigate directly to event details (no selection, no highlight)
                        goToDetails(eventId);
                    }

                    @Override
                    public void onEventSelected(String eventId) {
                        goToDetails(eventId);
                    }


                }
        );
        binding.rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEvents.setAdapter(adapter);
        binding.rvEvents.setNestedScrollingEnabled(false); // For smooth scrolling
    }

    /**
     * It decides which data loading method to call.
     */
    private void loadEventsBasedOnRole() {
        if (isAdmin) {
            binding.tvTitle.setText("All Events (Admin)");
            loadAllEventsForAdmin();
        } else {
            binding.tvTitle.setText("Open Events");
            loadEventsForUser();
        }
    }

    /**
     * LOADS DATA FOR A REGULAR USER
     */
    private void loadEventsForUser() {
        manager.loadOpenEventsForUser(userName, new EntrantEventManager.EventsCallback() {
            @Override
            public void onSuccess(List<EntrantEventManager.EventModel> list,
                                  List<String> waitlisted) {
                updateAdapterWithEvents(list);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * LOADS ALL EVENTS FOR AN ADMIN
     */
    private void loadAllEventsForAdmin() {
        manager.loadAllOpenEvents(new EntrantEventManager.EventsCallback() {
            @Override
            public void onSuccess(List<EntrantEventManager.EventModel> list, List<String> waitlisted) {
                updateAdapterWithEvents(list);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error loading admin events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Helper method to prevent code duplication.
     * Updates the RecyclerView adapter with a new list of events.
     */
    private void updateAdapterWithEvents(List<EntrantEventManager.EventModel> eventModelList) {
        eventItems.clear();
        for (EntrantEventManager.EventModel evt : eventModelList) {
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
        Log.d("HomeScreen", "Adapter updated with " + eventItems.size() + " events.");
    }

    // Navigate directly to the event details
    private void goToDetails(String eventId) {
        NavHostFragment.findNavController(this)
                .navigate(HomeScreenDirections.actionHomeScreenToEventDetailsScreen(userName, eventId));
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
