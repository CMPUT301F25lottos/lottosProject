package com.example.lottos.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottos.TimeUtils;
import com.example.lottos.databinding.FragmentOrganizerEventDetailsScreenBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A Fragment that displays a detailed, organizer-specific view of an event.
 *
 * Role: This class serves as the UI layer for an organizer managing a specific event.
 * It relies on the {@link OrganizerEventDetailsManager} for all business logic and
 * data fetching, and the {@link CsvExportManager} for data export functionality.
 * Its primary responsibilities are:
 * <ul>
 *     <li>Displaying core event details like name, date, and location.</li>
 *     <li>Showing lists of users categorized by their status (waitlist, selected, enrolled, etc.).</li>
 *     <li>Providing UI controls for organizer-specific actions, such as running the lottery
 *         or exporting the list of enrolled users to a CSV file.</li>
 *     <li>Controlling the visibility and state of UI elements based on the event's status
 *         (e.g., enabling the lottery button only after registration closes).</li>
 *     <li>Handling navigation to other parts of the app, including the user's profile and notifications.</li>
 * </ul>
 */
public class OrganizerEventDetailsScreen extends Fragment {

    private FragmentOrganizerEventDetailsScreenBinding binding;
    private OrganizerEventDetailsManager manager;
    private CsvExportManager csvExportManager;
    private String userName;
    private String eventId;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This method inflates the fragment's layout and initializes manager classes.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrganizerEventDetailsScreenBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            OrganizerEventDetailsScreenArgs args =
                    OrganizerEventDetailsScreenArgs.fromBundle(getArguments());
            userName = args.getUserName();
            eventId = args.getEventId();
        }

        manager = new OrganizerEventDetailsManager();
        csvExportManager = new CsvExportManager(requireContext());
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored into the view.
     * This method triggers the initial data load and sets up navigation button listeners.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadEvent();
        setupNavButtons();
        setupSectionToggles();

        binding.btnViewWaitlistMap.setOnClickListener(v -> handleViewWaitlistMap());
    }

    /**
     * Handles the click event for the "View Map" button.
     * Navigates to the GeoLocationMapScreen, passing the current event ID.
     */
    private void handleViewWaitlistMap() {
        NavHostFragment.findNavController(this)
                .navigate(OrganizerEventDetailsScreenDirections
                        .actionOrganizerEventDetailsScreenToGeoLocationMapScreen(eventId, userName));
    }

    /**
     * Initiates the process of loading the event's details from the manager.
     * It provides a callback to handle the successful retrieval of data or any errors.
     */
    private void loadEvent() {
        manager.loadEvent(eventId, new OrganizerEventDetailsManager.LoadCallback() {
            /**
             * On successful data load, this method updates the entire UI with the fetched event details
             * and user lists.
             */
            @Override
            public void onSuccess(Map<String, Object> eventData, List<String> waitlistUsers, List<String> selectedUsers, List<String> notSelectedUsers, List<String> enrolledUsers, List<String> cancelledUsers) {

                renderHeader(eventData);

                binding.tvWaitlistHeader.setText("Waitlist (" + waitlistUsers.size() + ")");
                binding.tvSelectedHeader.setText("Selected (" + selectedUsers.size() + ")");
                binding.tvNotSelectedHeader.setText("Not Selected (" + notSelectedUsers.size() + ")");
                binding.tvEnrolledHeader.setText("Enrolled (" + enrolledUsers.size() + ")");
                binding.tvCancelledHeader.setText("Cancelled (" + cancelledUsers.size() + ")");

                showRecyclerView(binding.rvWaitlist, binding.tvWaitlistEmpty, waitlistUsers);
                showRecyclerView(binding.rvSelected, binding.tvSelectedEmpty, selectedUsers);
                showRecyclerView(binding.rvNotSelected, binding.tvNotSelectedEmpty, notSelectedUsers);
                showRecyclerView(binding.rvEnrolled, binding.tvEnrolledEmpty, enrolledUsers);
                showRecyclerView(binding.rvCancelled, binding.tvCancelledEmpty, cancelledUsers);

                updateUI(eventData, waitlistUsers);
            }

            /**
             * On failure, displays a toast message with the error.
             */
            @Override
            public void onError(Exception e) {
                toast("Failed to load event: " + e.getMessage());
            }
        });
    }

    /**
     * Populates the header section of the UI with the event's name, location, date, and time.
     * @param data A map containing the raw event data from Firestore.
     */
    private void renderHeader(Map<String, Object> data) {
        if (data == null) return;

        binding.tvEventName.setText(safe(data.get("eventName")));
        binding.tvEventLocation.setText("Event Location: " + safe(data.get("location")));

        Date start = TimeUtils.toDate(data.get("startTime"));
        Date end = TimeUtils.toDate(data.get("endTime"));

        String dateText = "Date: N/A";
        String timeText = "Time: N/A";

        if (start != null && end != null) {
            SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

            String startDay = dayFmt.format(start);
            String endDay = dayFmt.format(end);
            String startTime = timeFmt.format(start);
            String endTime = timeFmt.format(end);

            dateText = "Date: " + startDay + " ~ " + endDay;
            timeText = "Time: " + startTime + " ~ " + endTime;
        }

        binding.tvEventDateTime.setText(dateText + " | " + timeText);

        if (start != null && end != null) {
            SimpleDateFormat regFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            String regText = "Registration Period: " +
                    regFmt.format(start) + " ~ " + regFmt.format(end);
            binding.tvRegisterPeriod.setText(regText);
        } else {
            binding.tvRegisterPeriod.setText("Registration Period: N/A");
        }
    }

    /**
     * Configures and populates a RecyclerView with a list of user names.
     * If the list of users is null or empty, it hides the RecyclerView and shows a "empty" label.
     * Otherwise, it sets up the LayoutManager and a UserListAdapter to display the names.
     *
     * @param rv         The RecyclerView instance to populate.
     * @param emptyLabel The TextView to display when the user list is empty.
     * @param users      The list of user strings to display in the RecyclerView.
     */
    private void showRecyclerView(RecyclerView rv, TextView emptyLabel, List<String> users) {

        if (users == null || users.isEmpty()) {
            rv.setVisibility(View.GONE);
            emptyLabel.setVisibility(View.VISIBLE);
            emptyLabel.setTag("empty");
            return;
        }

        emptyLabel.setVisibility(View.GONE);
        emptyLabel.setTag(null);
        rv.setVisibility(View.VISIBLE);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        rv.setAdapter(new UserListAdapter(users));
    }


    private void setupSectionToggles() {
        setupToggle(binding.sectionWaitlist, binding.iconWaitlistToggle, binding.rvWaitlist, binding.tvWaitlistEmpty);
        setupToggle(binding.sectionSelected, binding.iconSelectedToggle, binding.rvSelected, binding.tvSelectedEmpty);
        setupToggle(binding.sectionNotSelected, binding.iconNotSelectedToggle, binding.rvNotSelected, binding.tvNotSelectedEmpty);
        setupToggle(binding.sectionEnrolled, binding.iconEnrolledToggle, binding.rvEnrolled, binding.tvEnrolledEmpty);
        setupToggle(binding.sectionCancelled, binding.iconCancelledToggle, binding.rvCancelled, binding.tvCancelledEmpty);
    }

    /**
     * Sets up the click listener for a single collapsible section.
     * When the section header is clicked, it toggles the visibility of the RecyclerView and its
     * corresponding empty-state TextView. It also rotates the toggle icon to indicate an open or closed state.
     *
     * @param sectionLayout The layout view that serves as the click target (the section header).
     * @param icon          The ImageView for the expand/collapse arrow icon.
     * @param recycler      The RecyclerView that is shown or hidden.
     * @param emptyLabel    The TextView that is shown when the list is empty and the section is open.
     */
    private void setupToggle(View sectionLayout, ImageView icon, View recycler, View emptyLabel) {

        sectionLayout.setOnClickListener(v -> {
            boolean isOpen = recycler.getVisibility() == View.VISIBLE;

            if (isOpen) {
                recycler.setVisibility(View.GONE);
                emptyLabel.setVisibility(View.GONE);
                icon.setRotation(0);
            } else {
                recycler.setVisibility(View.VISIBLE);

                if ("empty".equals(emptyLabel.getTag())) {
                    emptyLabel.setVisibility(View.VISIBLE);
                }

                icon.setRotation(180);
            }
        });
    }

    /**
     * Updates organizer-specific UI elements based on the current event state.
     * This method controls the visibility and functionality of the "Run Lottery" and "Export CSV" buttons.
     *
     * @param eventData A map containing the event's data, such as its open status and lottery status.
     * @param waitUsers A list of users currently on the waitlist.
     */
    private void updateUI(Map<String, Object> eventData, List<String> waitUsers) {
        if (eventData == null) return;

        boolean isOpen = Boolean.TRUE.equals(eventData.get("IsOpen"));
        boolean hasRunLottery = Boolean.TRUE.equals(eventData.get("IsLottery"));
        String organizer = safe(eventData.get("organizer"));
        boolean isOrganizer = organizer.equalsIgnoreCase(userName);

        binding.btnLottery.setVisibility(View.GONE);
        binding.btnExportCsv.setVisibility(View.GONE);

        if (isOrganizer) {
            binding.btnExportCsv.setVisibility(View.VISIBLE);

            binding.btnExportCsv.setOnClickListener(v ->
                    csvExportManager.exportEnrolledUsers(eventData,
                            new CsvExportManager.CsvExportCallback() {
                                @Override
                                public void onSuccess(String path) {
                                    toast("CSV exported successfully");
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    toast("Export failed: " + errorMessage);
                                }
                            }));

            if (!hasRunLottery && waitUsers != null && !waitUsers.isEmpty()) {
                binding.btnLottery.setVisibility(View.VISIBLE);

                binding.btnLottery.setOnClickListener(v -> {
                    if (isOpen) {
                        toast("Registration is still open. You can run the lottery after it closes.");
                        return;
                    }

                    binding.btnLottery.setEnabled(false);

                    manager.runLottery(eventId, waitUsers,
                            () -> {
                                toast("Lottery completed");
                                binding.btnLottery.setEnabled(true);
                                loadEvent();
                            },
                            e -> {
                                toast("Lottery failed: " + e.getMessage());
                                binding.btnLottery.setEnabled(true);
                            });
                });
            }
        }
    }

    /**
     * A null-safe helper method to convert an object to its string representation.
     * @param o The object to convert.
     * @return The object's string value, or an empty string if the object is null.
     */
    private String safe(Object o) {
        return o == null ? "" : o.toString();
    }

    /**
     * Sets up the click listeners for the main bottom navigation bar buttons.
     * Configures navigation actions to move to other screens like Home, Profile, etc.
     */
    private void setupNavButtons() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventDetailsScreenDirections
                                .actionOrganizerEventDetailsScreenToHomeScreen(userName)));

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventDetailsScreenDirections
                                .actionOrganizerEventDetailsScreenToOrganizerEventsScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventDetailsScreenDirections
                                .actionOrganizerEventDetailsScreenToNotificationScreen(userName)));

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventDetailsScreenDirections
                                .actionOrganizerEventDetailsScreenToEventHistoryScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventDetailsScreenDirections
                                .actionOrganizerEventDetailsScreenToProfileScreen(userName)));
    }

    /**
     * A utility method to display a short toast message.
     * @param msg The message to display.
     */
    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
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
