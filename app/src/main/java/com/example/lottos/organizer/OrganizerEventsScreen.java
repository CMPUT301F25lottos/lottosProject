package com.example.lottos.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottos.EventListAdapter;
import com.example.lottos.EventRepository;
import com.example.lottos.databinding.FragmentOrganizerEventsScreenBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore; // 1. Add this import

import java.util.ArrayList;
import java.util.List;

/**
 * A Fragment that displays a list of events created by the current organizer.
 *
 * Role: This screen serves as the main dashboard for an event organizer.
 * It is responsible for:
 * <ul>
 *     <li>Fetching and displaying all events where the current user is the organizer.</li>
 *     <li>Using a RecyclerView and {@link EventListAdapter} to render the list of events.</li>
 *     <li>Handling user selections within the list to track which event is currently active.</li>
 *     <li>Providing navigation controls to other screens, such as:
 *         <ul>
 *             <li>{@link CreateEventScreen} to add a new event.</li>
 *             <li>{@link EditEventScreen} to modify the selected event.</li>
 *             <li>{@link OrganizerEventDetailsScreen} to view details and manage the selected event.</li>
 *             <li>Standard navigation to profile, notifications, and home.</li>
 *         </ul>
 *     </li>
 * </ul>
 * It relies on the {@link EventRepository} to query the database for the relevant events.
 */
public class OrganizerEventsScreen extends Fragment {
    private FragmentOrganizerEventsScreenBinding binding;
    private EventRepository repo;
    private String userName;
    private final List<EventListAdapter.EventItem> events = new ArrayList<>();
    private EventListAdapter adapter;
    private String selectedEventId = null;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where the layout is inflated and the view binding is initialized.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The root view for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrganizerEventsScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored into the view.
     * This method initializes the fragment's logic, including setting up the RecyclerView,
     * loading the initial event data, and configuring UI button listeners.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = OrganizerEventsScreenArgs.fromBundle(getArguments()).getUserName();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        repo = new EventRepository(db);

        RecyclerView rv = binding.rvOrganizerEvents;
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new EventListAdapter(events, new EventListAdapter.Listener() {
            @Override
            public void onEventClick(String eventId) {
                openOrganizerEventDetailsScreen(eventId);
            }
            @Override
            public void onEventSelected(String eventId) {
                selectedEventId = eventId;
                Toast.makeText(getContext(),
                        "Selected event", Toast.LENGTH_SHORT).show();
            }
        });

        rv.setAdapter(adapter);
        loadOrganizerEvents();
        setupNavButtons();
    }

    /**
     * Fetches the list of events organized by the current user from the EventRepository.
     * On success, it clears the current list, populates it with new data, and notifies the adapter.
     * It displays a toast message if no events are found or if an error occurs.
     */
    private void loadOrganizerEvents() {
        repo.getEventsByOrganizer(userName).get()
                .addOnSuccessListener(query -> {
                    events.clear();

                    for (QueryDocumentSnapshot doc : query) {

                        String id = doc.getId();
                        String name = doc.getString("eventName");
                        String location = doc.getString("location");

                        Timestamp startTs = doc.getTimestamp("startTime");
                        Timestamp endTs   = doc.getTimestamp("endTime");

                        String startTimeText = startTs != null ? formatTimestamp(startTs) : "";
                        String endTimeText   = endTs != null ? formatTimestamp(endTs) : "";

                        String posterUrl = doc.getString("posterUrl");

                        if (name != null) {
                            events.add(new EventListAdapter.EventItem(id, name, true, location, startTimeText, endTimeText, posterUrl
                            ));
                        }
                    }
                    adapter.notifyDataSetChanged();

                    if (events.isEmpty()) {
                        Toast.makeText(getContext(),
                                "You haven’t created any events yet.",
                                Toast.LENGTH_SHORT).show();
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to load events", e);
                    Toast.makeText(getContext(),
                            "Error loading events.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Navigates to the EditEventScreen, passing the username and the ID of the event to be edited.
     *
     * @param eventId The unique identifier of the event to edit.
     */
    private void openEditEventScreen(String eventId) {
        NavHostFragment.findNavController(this)
                .navigate(OrganizerEventsScreenDirections
                        .actionOrganizerEventsScreenToEditEventScreen(userName, eventId));
    }

    /**
     * Navigates to the OrganizerEventDetailsScreen, passing the username and the ID of the event to be viewed.
     *
     * @param eventId The unique identifier of the event to view.
     */
    private void openOrganizerEventDetailsScreen(String eventId) {
        NavHostFragment.findNavController(this)
                .navigate(OrganizerEventsScreenDirections
                        .actionOrganizerEventsScreenToOrganizerEventDetailsScreen(userName, eventId));
    }

    /**
     * Formats a Firebase Timestamp into a readable "yyyy/MM/dd HH:mm" string.
     *
     * @param ts The Timestamp to format.
     * @return A formatted date-time string, or an empty string if the timestamp is null.
     */
    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "";
        java.util.Date date = ts.toDate();
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Sets up the OnClickListeners for all navigation and action buttons on the screen.
     * This includes buttons for creating, editing, and viewing events, as well as the standard navigation bar.
     */
    private void setupNavButtons() {

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventsScreenDirections
                                .actionOrganizerEventsScreenToHomeScreen(userName))
        );

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventsScreenDirections
                                .actionOrganizerEventsScreenToNotificationScreen(userName))
        );

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventsScreenDirections
                                .actionOrganizerEventsScreenToProfileScreen(userName))
        );

        binding.btnCreateEvent.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventsScreenDirections
                                .actionOrganizerEventsScreenToCreateEventScreen(userName))
        );

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventsScreenDirections
                                .actionOrganizerEventsScreenToEventHistoryScreen(userName))
        );

        binding.btnEditEvent.setOnClickListener(v -> {
            if (selectedEventId == null) {
                Toast.makeText(getContext(),
                        "Please select an event first.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            openEditEventScreen(selectedEventId);
        });

        binding.btnViewEventDetails.setOnClickListener(v -> {
            if (selectedEventId == null) {
                Toast.makeText(getContext(),
                        "Please select an event first.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            openOrganizerEventDetailsScreen(selectedEventId);
        });
    }


    /**
     * Called when the view previously created by onCreateView has been detached from the fragment.
     * The view binding object is cleared here to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
