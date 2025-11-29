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
import com.example.lottos.R;
import com.example.lottos.databinding.FragmentHomeScreenBinding;
import com.example.lottos.events.EntrantEventManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Home Screen Fragment.
 * - Checks if user is Admin or regular User.
 * - Updates event statuses.
 * - Sweeps expired events: selectedList → cancelledList + notifications.
 * - Shows list of events based on user role.
 * - Handles navigation buttons.
 */
public class HomeScreen extends Fragment {

    private FragmentHomeScreenBinding binding;
    private EventStatusUpdater eventUpdater;
    private UserStatusUpdater userUpdater;
    private EntrantEventManager manager;
    private String userName;
    private boolean isAdmin = false;
    private final List<EventListAdapter.EventItem> eventItems = new ArrayList<>();
    private EventListAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = HomeScreenArgs.fromBundle(getArguments()).getUserName();

        SharedPreferences sharedPreferences = requireActivity()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);


        Log.d("HomeScreen", "onViewCreated: userName=" + userName + ", isAdmin=" + isAdmin);

        eventUpdater = new EventStatusUpdater();
        userUpdater = new UserStatusUpdater();
        manager = new EntrantEventManager();


        eventUpdater.updateEventStatuses(new EventStatusUpdater.UpdateListener() {
            @Override
            public void onUpdateSuccess(int updatedCount) {
                Log.d("HomeScreen", "eventUpdater.onUpdateSuccess, updatedCount=" + updatedCount);
                // 2) After that, sweep expired selections for all events
                runSelectionSweepThenLoadEvents();
            }

            @Override
            public void onUpdateFailure(String errorMessage) {
                Log.d("HomeScreen", "eventUpdater.onUpdateFailure: " + errorMessage);
                Toast.makeText(getContext(),
                        "Status update failed: " + errorMessage,
                        Toast.LENGTH_SHORT).show();

                runSelectionSweepThenLoadEvents();
            }
        });

        setupRecycler();
        setupNavButtons();
    }

    /**
     * Runs the global sweep (for all events, all users), then loads events.
     */
    private void runSelectionSweepThenLoadEvents() {
        userUpdater.sweepExpiredSelectedUsers(new UserStatusUpdater.UpdateListener() {
            @Override
            public void onUpdateSuccess(int updatedCount) {
                Log.d("HomeScreen", "Sweep success. Affected users: " + updatedCount);
                loadEventsBasedOnRole();
            }

            @Override
            public void onUpdateFailure(String errorMessage) {
                Log.e("HomeScreen", "Sweep FAILED: " + errorMessage);
                Toast.makeText(getContext(),
                        "Selection cleanup failed: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
                loadEventsBasedOnRole();
            }
        });
    }

    private void setupRecycler() {
        adapter = new EventListAdapter(eventItems, new EventListAdapter.Listener() {
            @Override
            public void onEventClick(String eventId) {
                goToDetails(eventId);
            }

            @Override
            public void onEventSelected(String eventId) {
                goToDetails(eventId);
            }
        });

        binding.rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEvents.setAdapter(adapter);
        binding.rvEvents.setNestedScrollingEnabled(false);
    }

    private void loadEventsBasedOnRole() {
        if (isAdmin) {
            binding.tvTitle.setText("All Events (Admin)");
            loadAllEventsForAdmin();
        } else {
            binding.tvTitle.setText("Open Events");
            loadEventsForUser();
        }
    }

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

    private void loadAllEventsForAdmin() {
        manager.loadAllOpenEvents(new EntrantEventManager.EventsCallback() {
            @Override
            public void onSuccess(List<EntrantEventManager.EventModel> list,
                                  List<String> waitlisted) {
                updateAdapterWithEvents(list);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error loading admin events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAdapterWithEvents(List<EntrantEventManager.EventModel> eventModelList) {
        eventItems.clear();

        for (EntrantEventManager.EventModel evt : eventModelList) {

            String posterUrl = evt.posterUrl;

            eventItems.add(
                    new EventListAdapter.EventItem(
                            evt.id,
                            evt.name,
                            evt.isOpen,
                            evt.location,
                            evt.startTime,
                            evt.endTime,
                            posterUrl
                    )
            );
        }

        adapter.notifyDataSetChanged();
        Log.d("HomeScreen", "Adapter updated with " + eventItems.size() + " events.");
    }

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

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections.actionHomeScreenToNotificationScreen(userName)));


        if (isAdmin) {

            binding.btnEventHistory.setImageResource(R.drawable.outline_article_person_24);
            binding.btnEventHistory.setOnClickListener(v ->
                    NavHostFragment.findNavController(HomeScreen.this)
                            .navigate(HomeScreenDirections.actionHomeScreenToViewUsersScreen(userName))
            );

            binding.btnOpenEvents.setImageResource(R.drawable.outline_add_photo_alternate_24);
            binding.btnOpenEvents.setOnClickListener(v ->

                    NavHostFragment.findNavController(HomeScreen.this)
                            .navigate(HomeScreenDirections.actionToAllImagesFragment(userName))
            );

        } else {

            binding.btnEventHistory.setImageResource(R.drawable.ic_history);
            binding.btnEventHistory.setOnClickListener(v ->
                    NavHostFragment.findNavController(HomeScreen.this)
                            .navigate(HomeScreenDirections.actionHomeScreenToEventHistoryScreen(userName)));


            binding.btnOpenEvents.setImageResource(R.drawable.ic_event);
            binding.btnOpenEvents.setOnClickListener(v ->
                    NavHostFragment.findNavController(HomeScreen.this)
                            .navigate(HomeScreenDirections.actionHomeScreenToOrganizerEventsScreen(userName)));
        }
    }








    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
