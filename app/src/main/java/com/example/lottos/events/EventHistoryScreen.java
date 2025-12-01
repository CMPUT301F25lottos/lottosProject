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

/**
 * A Fragment that displays a list of all events a user has interacted with.
 *
 * Role: This screen shows a comprehensive history, including events the user is
 * waitlisted for, enrolled in, was selected for, or declined. It fetches this
 * data using the EntrantEventManager and displays it in a RecyclerView.
 * It also provides navigation to other parts of the app, such as the event details
 * screen and the main home screen.
 */
public class EventHistoryScreen extends Fragment {

    private FragmentEventHistoryScreenBinding binding;
    private EntrantEventManager manager;
    private String userName;
    private final List<EventListAdapter.EventItem> eventItems = new ArrayList<>();
    private EventListAdapter adapter;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where the layout is inflated from its XML definition.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventHistoryScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored into the view.
     * This is where the fragment's logic is initialized, including setting up the RecyclerView,
     * loading event data, and configuring navigation buttons.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve the username passed from the previous screen
        userName = EventHistoryScreenArgs.fromBundle(getArguments()).getUserName();
        manager = new EntrantEventManager();

        setupRecycler();
        setupNavButtons();
        loadEventsForUser();
    }

    /**
     * Initializes the RecyclerView, its adapter, and its layout manager.
     * The adapter is configured with a listener to handle clicks on event items,
     * which navigates the user to the corresponding event details screen.
     */
    private void setupRecycler() {
        adapter = new EventListAdapter(
                eventItems,
                new EventListAdapter.Listener() {
                    /**
                     * Handles clicks on an event in the list.
                     * @param eventId The unique ID of the clicked event.
                     */
                    @Override
                    public void onEventClick(String eventId) {
                        goToDetails(eventId);
                    }
                    /**
                     * Handles the selection of an event in the list.
                     * @param eventId The unique ID of the selected event.
                     */
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

    /**
     * Initiates the process of loading the event history for the current user.
     * It uses the EntrantEventManager to fetch the data asynchronously and defines
     * callbacks to handle successful data retrieval or errors.
     */
    private void loadEventsForUser() {
        manager.loadEventsHistory(userName, new EntrantEventManager.EventsCallback() {
            /**
             * Callback executed on successful retrieval of event history.
             * @param list The list of event models related to the user.
             * @param waitlisted A list of event IDs the user is waitlisted for (unused in this context).
             */
            @Override
            public void onSuccess(List<EntrantEventManager.EventModel> list, List<String> waitlisted) {
                updateAdapterWithEvents(list);
            }

            /**
             * Callback executed when an error occurs during data fetching.
             * @param e The exception that occurred.
             */
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updates the RecyclerView's adapter with a new list of events.
     * It clears the existing items and repopulates the list with data from the provided
     * event models, then notifies the adapter that the dataset has changed.
     *
     * @param eventModelList The new list of event models to display.
     */
    private void updateAdapterWithEvents(List<EntrantEventManager.EventModel> eventModelList) {
        eventItems.clear();

        for (EntrantEventManager.EventModel evt : eventModelList) {
            // Convert the data model to a UI-specific item
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

    /**
     * Navigates to the EventDetailsScreen for a specific event.
     *
     * @param eventId The unique identifier of the event to display details for.
     */
    private void goToDetails(String eventId) {
        NavHostFragment.findNavController(this)
                .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToEventDetailsScreen(userName, eventId));
    }

    /**
     * Sets up the OnClickListeners for all navigation buttons in the fragment's layout.
     * This includes buttons for navigating back to the home screen, to notifications,
     * to the user profile, and to the organizer's event view.
     */
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
