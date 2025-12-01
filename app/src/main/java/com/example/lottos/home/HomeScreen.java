package com.example.lottos.home;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lottos.EventListAdapter;
import com.example.lottos.R;
import com.example.lottos.databinding.FragmentHomeScreenBinding;
import com.example.lottos.events.EntrantEventManager;

import java.util.ArrayList;
import java.util.Calendar;
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

    // Full event list from Firestore (source of truth for filtering)
    private final List<EntrantEventManager.EventModel> allEvents = new ArrayList<>();

    // Current interest filters + availability filters
    private final List<String> selectedKeywordFilters = new ArrayList<>();
    private Long availabilityFromMillis = null;
    private Long availabilityToMillis = null;

    // List used by the RecyclerView adapter
    private final List<EventListAdapter.EventItem> eventItems = new ArrayList<>();
    private EventListAdapter adapter;

    // All possible keywords for filtering.
    private static final String[] FILTER_KEYWORDS = new String[] {
            "Sports",
            "Music",
            "Food",
            "Arts and Crafts",
            "Charity",
            "Cultural",
            "Workshop",
            "Party",
            "Study",
            "Networking",
            "Family",
            "Seniors",
            "Teens",
            "Health",
            "Kids",
            "Movie",
            "Other"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = HomeScreenArgs.fromBundle(getArguments()).getUserName();

        SharedPreferences sharedPreferences = requireActivity()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);

        Log.d("HomeScreen", "onViewCreated: userName=" + userName + ", isAdmin=" + isAdmin);

        eventUpdater = new EventStatusUpdater();
        userUpdater = new UserStatusUpdater();
        manager = new EntrantEventManager();

        setupRecycler();
        setupNavButtons();     // includes btnFilter wiring

        // Update statuses → sweep expired selections → then load events
        eventUpdater.updateEventStatuses(new EventStatusUpdater.UpdateListener() {
            @Override
            public void onUpdateSuccess(int updatedCount) {
                Log.d("HomeScreen", "eventUpdater.onUpdateSuccess, updatedCount=" + updatedCount);
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

                allEvents.clear();
                allEvents.addAll(list);

                // Reset filters when reloading
                selectedKeywordFilters.clear();
                availabilityFromMillis = null;
                availabilityToMillis = null;

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

                allEvents.clear();
                allEvents.addAll(list);

                // Reset filters when reloading
                selectedKeywordFilters.clear();
                availabilityFromMillis = null;
                availabilityToMillis = null;

                updateAdapterWithEvents(list);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error loading admin events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Converts EventModel list → EventItem list for the adapter.
     */
    private void updateAdapterWithEvents(List<EntrantEventManager.EventModel> eventModelList) {
        eventItems.clear();

        for (EntrantEventManager.EventModel evt : eventModelList) {

            String posterUrl = evt.posterUrl;

            eventItems.add(
                    new EventListAdapter.EventItem(evt.id, evt.name, evt.isOpen, evt.location, evt.startTime, evt.endTime, posterUrl)
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

    /**
     * Applies both interest (keywords) and availability filters and updates the list.
     */
    private void applyAllFiltersAndUpdate() {
        // 1) filter by interests
        List<EntrantEventManager.EventModel> filtered =
                manager.filterEventsByKeywords(allEvents, selectedKeywordFilters);

        // 2) filter by availability
        filtered = manager.filterEventsByAvailability(
                filtered,
                availabilityFromMillis,
                availabilityToMillis
        );

        // 3) push to adapter
        updateAdapterWithEvents(filtered);
    }

    private void setupNavButtons() {

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections
                                .actionHomeScreenToProfileScreen(userName)));

        binding.btnInfo.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections
                                .actionHomeScreenToLotteryInfoScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections
                                .actionHomeScreenToNotificationScreen(userName)));

        // Filter button: keywords + availability via one dialog
        binding.btnFilter.setOnClickListener(v -> showFilterDialog());

        if (isAdmin) {

            binding.btnEventHistory.setImageResource(R.drawable.outline_article_person_24);
            binding.btnEventHistory.setOnClickListener(v ->
                    NavHostFragment.findNavController(HomeScreen.this)
                            .navigate(HomeScreenDirections
                                    .actionHomeScreenToViewUsersScreen(userName))
            );

            binding.btnOpenEvents.setImageResource(R.drawable.outline_add_photo_alternate_24);
            binding.btnOpenEvents.setOnClickListener(v ->
                    NavHostFragment.findNavController(HomeScreen.this)
                            .navigate(HomeScreenDirections
                                    .actionToAllImagesFragment(userName))
            );

        } else {

            binding.btnEventHistory.setImageResource(R.drawable.ic_history);
            binding.btnEventHistory.setOnClickListener(v ->
                    NavHostFragment.findNavController(HomeScreen.this)
                            .navigate(HomeScreenDirections
                                    .actionHomeScreenToEventHistoryScreen(userName)));

            binding.btnOpenEvents.setImageResource(R.drawable.ic_event);
            binding.btnOpenEvents.setOnClickListener(v ->
                    NavHostFragment.findNavController(HomeScreen.this)
                            .navigate(HomeScreenDirections
                                    .actionHomeScreenToOrganizerEventsScreen(userName)));
        }
    }

    /**
     * Unified filter dialog for keywords + date range.
     */
    private void showFilterDialog() {
        if (allEvents.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No events to filter.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = FILTER_KEYWORDS;
        boolean[] checked = new boolean[items.length];

        // Pre-check already selected keywords
        for (int i = 0; i < items.length; i++) {
            if (selectedKeywordFilters.contains(items[i])) {
                checked[i] = true;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Filter events")
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setPositiveButton("Apply", (dialog, which) -> {
                    // Apply only keyword filters
                    selectedKeywordFilters.clear();
                    for (int i = 0; i < items.length; i++) {
                        if (checked[i]) {
                            selectedKeywordFilters.add(items[i]);
                        }
                    }
                    applyAllFiltersAndUpdate();
                })
                .setNegativeButton("Clear all", (dialog, which) -> {
                    // Clear both keyword + date filters
                    selectedKeywordFilters.clear();
                    availabilityFromMillis = null;
                    availabilityToMillis = null;
                    applyAllFiltersAndUpdate();
                })
                .setNeutralButton("Dates…", (dialog, which) -> {
                    // Save keyword selection, then open date picker
                    selectedKeywordFilters.clear();
                    for (int i = 0; i < items.length; i++) {
                        if (checked[i]) {
                            selectedKeywordFilters.add(items[i]);
                        }
                    }
                    showDateFilterDialog();
                })
                .show();
    }

    /**
     * Shows a two-step date picker dialog (from / to).
     * Updates availabilityFromMillis / availabilityToMillis and applies filters.
     */
    private void showDateFilterDialog() {
        Calendar cal = Calendar.getInstance();
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        // Pick FROM date
        DatePickerDialog fromDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {

                    Calendar fromCal = Calendar.getInstance();
                    fromCal.set(year, month, dayOfMonth, 0, 0, 0);
                    availabilityFromMillis = fromCal.getTimeInMillis();

                    // Now pick TO date
                    DatePickerDialog toDialog = new DatePickerDialog(
                            requireContext(),
                            (view2, year2, month2, dayOfMonth2) -> {

                                Calendar toCal = Calendar.getInstance();
                                toCal.set(year2, month2, dayOfMonth2, 23, 59, 59);
                                availabilityToMillis = toCal.getTimeInMillis();

                                applyAllFiltersAndUpdate();

                            }, year, month, dayOfMonth);
                    toDialog.setTitle("Select end date");
                    toDialog.show();

                }, y, m, d);
        fromDialog.setTitle("Select start date");
        fromDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
