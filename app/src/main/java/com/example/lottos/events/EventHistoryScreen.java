package com.example.lottos.events;

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
import com.example.lottos.databinding.FragmentEventHistoryScreenBinding;
import com.example.lottos.events.EntrantEventManager;
import com.example.lottos.home.HomeScreenArgs;
import com.example.lottos.home.HomeScreenDirections;

import java.util.ArrayList;
import java.util.List;

public class EventHistoryScreen extends Fragment {

    private FragmentEventHistoryScreenBinding binding;
    private EntrantEventManager manager;
    private String userName;
    private final List<EventListAdapter.EventItem> eventItems = new ArrayList<>();
    private EventListAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventHistoryScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = HomeScreenArgs.fromBundle(getArguments()).getUserName();
        manager = new EntrantEventManager();

        setupRecycler();
        setupNavButtons();
        loadEventsForUser();
    }

    private void setupRecycler() {
        adapter = new EventListAdapter(
                eventItems,
                new EventListAdapter.Listener() {
                    @Override
                    public void onEventClick(String eventId) {
                        goToDetails(eventId);
                    }
                    @Override
                    public void onEventSelected(String eventId) {
                        goToDetails(eventId);
                    }
                }
        );

        binding.rvEventHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEventHistory.setAdapter(adapter);
        binding.rvEventHistory.setNestedScrollingEnabled(false);
    }

    private void loadEventsForUser() {
        manager.loadEventsHistory(userName, new EntrantEventManager.EventsCallback() {
            @Override
            public void onSuccess(List<EntrantEventManager.EventModel> list, List<String> waitlisted) {
                updateAdapterWithEvents(list);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }

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
                            evt.endTime,
                            evt.posterUrl
                    )
            );
        }

        adapter.notifyDataSetChanged();
        Log.d("EventHistory", "Adapter updated with " + eventItems.size() + " events.");
    }


    private void goToDetails(String eventId) {
        NavHostFragment.findNavController(this)
                .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToEventDetailsScreen(userName, eventId));
    }

    private void setupNavButtons() {

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToHomeScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToNotificationScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToProfileScreen(userName)));

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToOrganizerEventsScreen(userName))
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
