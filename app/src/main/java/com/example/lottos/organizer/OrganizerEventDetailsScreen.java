package com.example.lottos.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Import this
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.TimeUtils;
import com.example.lottos.databinding.FragmentOrganizerEventDetailsScreenBinding;
import com.example.lottos.organizer.CsvExportManager; // Import this

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
    private CsvExportManager csvExportManager; // <-- 1. Add CsvExportManager field
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
        csvExportManager = new CsvExportManager(requireContext()); // <-- 2. Initialize the manager
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored into the view.
     * This method triggers the initial data load and sets up navigation button listeners.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) { // Make sure @Nullable is imported
        super.onViewCreated(view, savedInstanceState);
        loadEvent();
        setupNavButtons();

        binding.btnViewWaitlistMap.setOnClickListener(v -> handleViewWaitlistMap());
    }

    /**
     * Handles the click event for the "View Map" button.
     * Navigates to the GeoLocationMapScreen, passing the current event ID.
     */
    private void handleViewWaitlistMap() {
        // Navigate to the map view, passing the event ID
        NavHostFragment.findNavController(this)
                .navigate(OrganizerEventDetailsScreenDirections
                        .actionOrganizerEventDetailsScreenToGeoLocationMapScreen(eventId));
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

                renderListSection(binding.tvWaitlistEmpty,  waitlistUsers,  "No users on waitlist.");
                renderListSection(binding.tvSelectedEmpty,  selectedUsers,  "No selected users yet.");
                renderListSection(binding.tvNotSelectedEmpty, notSelectedUsers, "No not-selected users yet.");
                renderListSection(binding.tvEnrolledEmpty,  enrolledUsers,  "No enrolled users yet.");
                renderListSection(binding.tvCancelledEmpty, cancelledUsers, "No cancelled users yet.");

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
        binding.tvEventLocation.setText("Location: " + safe(data.get("location")));

        Date start = TimeUtils.toDate(data.get("startTime"));
        Date end   = TimeUtils.toDate(data.get("endTime"));

        String dateText = "Date: N/A";
        String timeText = "Time: N/A";

        if (start != null && end != null) {

            SimpleDateFormat dayFmt  = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

            String startDay   = dayFmt.format(start);
            String endDay     = dayFmt.format(end);
            String startTime  = timeFmt.format(start);
            String endTime    = timeFmt.format(end);

            dateText = "Date: " + startDay + " ~ " + endDay;

            timeText = "Time: " + startTime + " ~ " + endTime;
        }

        binding.tvEventDate.setText(dateText);
        binding.tvEventTime.setText(timeText);
    }


    /**
     * Renders a list of users into a designated TextView.
     * If the list is empty, it displays a provided empty message instead.
     * @param contentView The TextView where the user list or empty message will be displayed.
     * @param users The list of user names to display.
     * @param emptyMessage The message to show if the user list is null or empty.
     */
    private void renderListSection(TextView contentView, List<String> users, String emptyMessage) {

        if (contentView == null) return;

        if (users == null || users.isEmpty()) {
            contentView.setVisibility(View.VISIBLE);
            contentView.setText(emptyMessage);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String u : users) {
            sb.append("• ").append(u).append("\n");
        }

        contentView.setVisibility(View.VISIBLE);
        contentView.setText(sb.toString().trim());
    }

    /**
     * Updates the state of interactive UI elements based on the event's status.
     * This includes showing/hiding the lottery and export buttons and setting their click listeners.
     * @param eventData The map containing the raw event data.
     * @param waitUsers The current list of users on the waitlist.
     */
    private void updateUI(Map<String, Object> eventData, List<String> waitUsers) {

        if (eventData == null) return;

        boolean isOpen     = Boolean.TRUE.equals(eventData.get("IsOpen"));
        boolean hasRunLottery = Boolean.TRUE.equals(eventData.get("IsLottery"));
        String organizer   = safe(eventData.get("organizer"));
        boolean isOrganizer = organizer.equalsIgnoreCase(userName);

        binding.btnLottery.setVisibility(View.GONE);
        binding.btnLottery.setOnClickListener(null);
        binding.btnExportCsv.setVisibility(View.GONE); // <-- 3. Hide export button by default
        binding.btnExportCsv.setOnClickListener(null);

        if (isOrganizer) { // Actions for the organizer
            binding.btnExportCsv.setVisibility(View.VISIBLE); // <-- 4. Show export button for organizer
            binding.btnExportCsv.setOnClickListener(v -> {
                csvExportManager.exportEnrolledUsers(eventData, new CsvExportManager.CsvExportCallback() {
                    @Override
                    public void onSuccess(String path) {
                        toast("CSV exported successfully");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        toast("Export failed: " + errorMessage);
                    }
                });
            });

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
     * Sets up the OnClickListeners for all the navigation buttons in the bottom bar.
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
