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
 * A Fragment representing the main home screen of the application.
 *
 * Role: This class serves as the central hub for users after they log in. Its key responsibilities include:
 * <ul>
 *     <li>Differentiating between an Admin and a regular User to tailor the UI and data presentation.</li>
 *     <li>Initiating background tasks to automatically update the "IsOpen" status of events based on their deadlines.</li>
 *     <li>Triggering a global "sweep" to handle users who did not respond to event selections in time.</li>
 *     <li>Displaying a list of events (all events for admins, open events for regular users) in a RecyclerView.</li>
 *     <li>Providing UI for filtering events by keywords and date ranges.</li>
 *     <li>Handling navigation to other parts of the application, such as user profiles, notifications, and event details.</li>
 * </ul>
 */
public class HomeScreen extends Fragment {

    private FragmentHomeScreenBinding binding;
    private EventStatusUpdater eventUpdater;
    private UserStatusUpdater userUpdater;
    private EntrantEventManager manager;
    private String userName;
    private boolean isAdmin = false;

    private final List<EntrantEventManager.EventModel> allEvents = new ArrayList<>();

    private final List<String> selectedKeywordFilters = new ArrayList<>();
    private Long availabilityFromMillis = null;
    private Long availabilityToMillis = null;

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

    /**
     * Called to have the fragment instantiate its user interface view.
     * This method inflates the fragment's layout.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored into the view.
     * This method initializes the fragment's components, retrieves user data, and starts the data loading process.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
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
        setupNavButtons();

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
     * Chains the global user sweep operation to run after event statuses are updated,
     * and then proceeds to load the event list.
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

    /**
     * Initializes the RecyclerView, its adapter, and its LayoutManager.
     * The adapter is configured with a listener to handle clicks on event items.
     */
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

    /**
     * Determines whether to load events for an administrator or a regular user
     * and calls the appropriate loading method.
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
     * Fetches the list of currently open events for a regular user.
     * On success, it populates the main event list and updates the adapter.
     */
    private void loadEventsForUser() {
        manager.loadOpenEventsForUser(userName, new EntrantEventManager.EventsCallback() {
            @Override
            public void onSuccess(List<EntrantEventManager.EventModel> list,
                                  List<String> waitlisted) {

                allEvents.clear();
                allEvents.addAll(list);

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

    /**
     * Fetches a complete list of all events, regardless of status, for an administrator.
     * On success, it populates the main event list and updates the adapter.
     */
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
     * Clears the current list of displayable event items and repopulates it based on a new
     * list of data models, then notifies the RecyclerView adapter of the change.
     * @param eventModelList The new list of event data models to display.
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

    /**
     * Navigates to the event details screen for a specific event.
     * @param eventId The unique ID of the event to view.
     */
    private void goToDetails(String eventId) {
        NavHostFragment.findNavController(this)
                .navigate(HomeScreenDirections
                        .actionHomeScreenToEventDetailsScreen(userName, eventId));
    }

    /**
     * Applies the currently selected keyword and availability filters to the master
     * list of events and updates the RecyclerView to show the filtered result.
     */
    private void applyAllFiltersAndUpdate() {
        List<EntrantEventManager.EventModel> filtered =
                manager.filterEventsByKeywords(allEvents, selectedKeywordFilters);

        filtered = manager.filterEventsByAvailability(
                filtered,
                availabilityFromMillis,
                availabilityToMillis
        );

        updateAdapterWithEvents(filtered);
    }

    /**
     * Sets up the click listeners for all navigation and action buttons in the fragment.
     * The behavior of some buttons is adjusted based on whether the user is an admin.
     */
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
     * Displays a multi-functional dialog that allows the user to filter events.
     * The dialog provides options for selecting keyword filters, applying them, clearing all filters,
     * or proceeding to a date-range selection.
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
     * Displays a sequence of two date picker dialogs to select a "from" and "to" date range.
     * After both dates are selected, it updates the availability filter properties and applies all filters.
     */
    private void showDateFilterDialog() {
        Calendar cal = Calendar.getInstance();
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog fromDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {

                    Calendar fromCal = Calendar.getInstance();
                    fromCal.set(year, month, dayOfMonth, 0, 0, 0);
                    availabilityFromMillis = fromCal.getTimeInMillis();

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

    /**
     * Called when the view previously created by onCreateView has been detached from the fragment.
     * This is where the view binding object is cleared to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
