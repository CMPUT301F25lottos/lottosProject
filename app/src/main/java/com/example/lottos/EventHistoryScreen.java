//package com.example.lottos;
//
//import android.content.Context;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.navigation.fragment.NavHostFragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//
//import com.example.lottos.databinding.FragmentEventHistoryScreenBinding;
//import com.example.lottos.events.EntrantEventManager;
//import com.example.lottos.home.EventStatusUpdater;
//import com.example.lottos.home.HomeScreenArgs;
//import com.example.lottos.home.HomeScreenDirections;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class EventHistoryScreen extends Fragment {
//
//    private FragmentEventHistoryScreenBinding binding;
//    private EventStatusUpdater eventUpdater;
//    private EntrantEventManager manager;
//
//    private String userName;
//
//    private final List<EventListAdapter.EventItem> eventItems = new ArrayList<>();
//    private EventListAdapter adapter;
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        binding = FragmentEventHistoryScreenBinding.inflate(inflater, container, false);
//        return binding.getRoot();
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        userName = HomeScreenArgs.fromBundle(getArguments()).getUserName();
//
//        eventUpdater = new EventStatusUpdater();
//        manager = new EntrantEventManager();
//
//        binding.tvTitle.setText("Event History");
//
//        eventUpdater.updateEventStatuses(new EventStatusUpdater.UpdateListener() {
//            @Override
//            public void onUpdateSuccess(int updatedCount) {
//                loadEventsForUser();
//            }
//
//            @Override
//            public void onUpdateFailure(String errorMessage) {
//                Toast.makeText(getContext(), "Status update failed: " + errorMessage, Toast.LENGTH_SHORT).show();
//                loadEventsForUser();
//            }
//        });
//
//        setupRecycler();
//        setupNavButtons();
//    }
//
//    private void setupRecycler() {
//        adapter = new EventListAdapter(
//                eventItems,
//                new EventListAdapter.Listener() {
//                    @Override
//                    public void onEventClick(String eventId) {
//                        goToDetails(eventId);
//                    }
//
//                    @Override
//                    public void onEventSelected(String eventId) {
//                        goToDetails(eventId);
//                    }
//                }
//        );
//
//        binding.rvEventHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
//        binding.rvEventHistory.setAdapter(adapter);
//        binding.rvEventHistory.setNestedScrollingEnabled(false);
//    }
//
//    private void loadEventsForUser() {
//        manager.loadEventsForUser(userName, new EntrantEventManager.EventsCallback() {
//            @Override
//            public void onSuccess(List<EntrantEventManager.EventModel> list,
//                                  List<String> waitlisted) {
//                updateAdapterWithEvents(list);
//            }
//
//            @Override
//            public void onError(Exception e) {
//                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void updateAdapterWithEvents(List<EntrantEventManager.EventModel> eventModelList) {
//        eventItems.clear();
//        for (EntrantEventManager.EventModel evt : eventModelList) {
//            eventItems.add(
//                    new EventListAdapter.EventItem(
//                            evt.id,
//                            evt.name,
//                            evt.isOpen,
//                            evt.location,
//                            evt.startTime,
//                            evt.endTime
//                    )
//            );
//        }
//        adapter.notifyDataSetChanged();
//        Log.d("EventHistory", "Adapter updated with " + eventItems.size() + " events.");
//    }
//
//    private void goToDetails(String eventId) {
//        NavHostFragment.findNavController(this)
//                .navigate(HomeScreenDirections.actionHomeScreenToEventDetailsScreen(userName, eventId));
//    }
//
//    private void setupNavButtons() {
//
//        binding.btnBack.setOnClickListener(v ->
//                NavHostFragment.findNavController(this)
//                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToHomeScreen(userName)));
//
//        binding.btnNotification.setOnClickListener(v ->
//                NavHostFragment.findNavController(this)
//                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToNotificationScreen(userName)));
//
//        binding.btnProfile.setOnClickListener(v ->
//                NavHostFragment.findNavController(this)
//                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToProfileScreen(userName)));
//
//        binding.btnOpenEvents.setOnClickListener(v ->
//                NavHostFragment.findNavController(this)
//                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToOrganizerEventsScreen(userName))
//        );
//    }
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        binding = null;
//    }
//}
