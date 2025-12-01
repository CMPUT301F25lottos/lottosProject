package com.example.lottos.events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.lottos.account.ProfileScreen;
import com.example.lottos.account.ProfileScreenDirections;
import com.google.firebase.Timestamp;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.events.EventDetailsScreenArgs;
import com.example.lottos.events.EventDetailsScreenDirections;
import com.example.lottos.databinding.FragmentEventDetailsScreenBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * UI-only Fragment for event details.
 * All Firestore & business logic is delegated to EventDetailsManager.
 */
public class EventDetailsScreen extends Fragment {

    private FragmentEventDetailsScreenBinding binding;
    private String eventName;
    private String userName;

    private EventDetailsManager manager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentEventDetailsScreenBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            EventDetailsScreenArgs args = EventDetailsScreenArgs.fromBundle(getArguments());
            eventName = args.getEventName();
            userName = args.getUserName();
        }

        manager = new EventDetailsManager();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupNavButtons();
        hideAllButtons();
        loadEvent();
    }

    // ------------------------------------------------------------------
    // LOAD + RENDER
    // ------------------------------------------------------------------
    private void loadEvent() {
        manager.loadEventForEntrant(eventName, userName,
                new EventDetailsManager.LoadCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> eventData,
                                          List<String> waitUsers,
                                          Map<String, Object> userData) {

                        renderEventData(eventData);
                        updateUI(eventData, waitUsers, userData);
                    }

                    @Override
                    public void onError(Exception e) {
                        toast("Failed to load event: " + e.getMessage());
                    }
                });
    }

    private void renderEventData(Map<String, Object> data) {
        binding.tvOrganizer.setText("Organizer: " + safe(data.get("organizer")));
        binding.tvEventName.setText(safe(data.get("eventName")));
        binding.tvLocation.setText(" | " + safe(data.get("location")));

        Object startTimeObj = data.get("startTime");

        if (startTimeObj instanceof Timestamp) {
            Timestamp ts = (Timestamp) startTimeObj;

            // Convert to java.util.Date
            Date date = ts.toDate();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            String formatted = sdf.format(date);

            binding.tvStartTime.setText(formatted);
        } else {
            binding.tvStartTime.setText("N/A");
        }


        Object endTimeObj = data.get("endTime");

        if (endTimeObj instanceof Timestamp) {
            Timestamp ts = (Timestamp) endTimeObj;

            // Convert to java.util.Date
            Date date = ts.toDate();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            String formatted = sdf.format(date);

            binding.tvEndTime.setText(" ~ " + formatted);
        } else {
            binding.tvEndTime.setText("N/A");
        }

        Object registercloseTimeObj = data.get("registerEndTime");

        if (registercloseTimeObj instanceof Timestamp) {
            Timestamp ts = (Timestamp) registercloseTimeObj;

            Date date = ts.toDate();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            String formatted = sdf.format(date);

            binding.tvWLCloseDateTime.setText("Register End Date: " + formatted);
        } else {
            binding.tvWLCloseDateTime.setText("Register End Date: N/A");
        }


        Map<String, Object> waitList = (Map<String, Object>) data.get("waitList");

        if (waitList != null) {
            List<?> users = (List<?>) waitList.get("users");

            int userCount = (users != null) ? users.size() : 0;

            binding.tvWLCount.setText("Number of Entrants on WaitList:" + userCount);
        }


        binding.tvDescription.setText("Description: " + safe(data.get("description")));

        binding.tvCapacity.setText("Event Capacity: " + safe(data.get("selectionCap")));

        Object wlCapacityObj = waitList.get("waitListCapacity");

        String capacity = "No Restriction";  // default string

        if (wlCapacityObj instanceof Integer) {
            capacity = String.valueOf(((Number) wlCapacityObj).intValue());
        }

        binding.tvWLSize.setText("Wait List Capacity: " + capacity);

    }
    private void exportCsv(Map<String, Object> eventData,
                           List<String> waitUsers,
                           Map<String, Object> userData) {

        Map<String, Object> enrolled = (Map<String, Object>) eventData.get("enrolledList");
        List<String> enrolledUsers = new ArrayList<>();

        if (enrolled != null && enrolled.get("users") instanceof List) {
            enrolledUsers = (List<String>) enrolled.get("users");
        }

        if (enrolledUsers.isEmpty()) {
            toast("No enrolled users to export.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Event Name,")
                .append("Organizer,")
                .append("Total Enrolled\n");

        sb.append(safe(eventData.get("eventName"))).append(",")
                .append(safe(eventData.get("organizer"))).append(",")
                .append(enrolledUsers.size()).append("\n\n");

        sb.append("Enrolled Users\n");

        for (String user : enrolledUsers) {
            sb.append(user).append("\n");
        }

        String fileName = safe(eventData.get("eventName")) + "_enrolled.csv";

        try {
            // Save file to external storage
            File file = new File(requireContext().getExternalFilesDir(null), fileName);
            FileWriter writer = new FileWriter(file);
            writer.append(sb.toString());
            writer.flush();
            writer.close();

            toast("CSV exported: " + file.getAbsolutePath());
            shareCsvFile(file);

        } catch (Exception e) {
            toast("Export failed: " + e.getMessage());

        }
    }
    private void shareCsvFile(File file) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Share CSV"));
    }


    private String safe(Object o) {
        return o == null ? "" : o.toString();
    }

    // ------------------------------------------------------------------
    // UI STATE
    // ------------------------------------------------------------------
    private void updateUI(Map<String, Object> eventData,
                          List<String> waitUsers,
                          Map<String, Object> userData) {

        boolean isOpen = Boolean.TRUE.equals(eventData.get("IsOpen"));
        boolean isLottery = Boolean.TRUE.equals(eventData.get("IsLottery"));

        String organizer = safe(eventData.get("organizer"));
        boolean isOrganizer = organizer.equalsIgnoreCase(userName);

        List<String> selectedEvents = readUserList(userData, "selectedEvents");
        List<String> waitlistedEvents = readUserList(userData, "waitListedEvents");

        boolean isWaitlisted = waitlistedEvents.contains(eventName);
        boolean isSelected = selectedEvents.contains(eventName);
        if (isOrganizer) {
            binding.btnExportCsv.setVisibility(View.VISIBLE);
            binding.btnExportCsv.setOnClickListener(v -> {
                exportCsv(eventData, waitUsers, userData);
            });
        }
        binding.btnBack.setVisibility(View.VISIBLE);

        // Organizer lottery button:
        // - visible for organizer as long as lottery not done
        // - only runs when event is closed (isOpen == false)
        if (isOrganizer && !isLottery) {
            binding.btnLottery.setVisibility(View.VISIBLE);
            binding.btnLottery.setOnClickListener(v -> {
                if (isOpen) {
                    toast("Registration is still open. You can run the lottery after it closes.");
                    return;
                }

                binding.btnLottery.setEnabled(false);

                manager.runLottery(eventName, waitUsers,
                        () -> {
                            toast("Lottery completed");
                            binding.btnLottery.setEnabled(true);
                            loadEvent(); // refresh view
                        },
                        e -> {
                            toast("Lottery failed: " + e.getMessage());
                            binding.btnLottery.setEnabled(true);
                        });
            });
        }
        if (isOrganizer) {
            binding.btnEditEvent.setVisibility(View.VISIBLE);
            binding.btnEditEvent.setOnClickListener(v -> openEditEvent());
        }
        // Entrant join/leave
        if (isOpen) {
            binding.btnJoin.setVisibility(View.VISIBLE);
            binding.btnJoin.setText(isWaitlisted ? "Leave Waitlist" : "Join Waitlist");

            binding.btnJoin.setOnClickListener(v -> {
                if (isWaitlisted) {
                    leaveWaitlist();
                } else {
                    joinWaitlist();
                }
            });
        }

        // Entrant accept/decline after selection
        if (!isOpen && isSelected) {
            binding.btnAccept.setVisibility(View.VISIBLE);
            binding.btnDecline.setVisibility(View.VISIBLE);

            binding.btnAccept.setOnClickListener(v -> acceptInvite());
            binding.btnDecline.setOnClickListener(v -> declineInvite());
        }
    }
    private void openEditEvent() {
        NavHostFragment.findNavController(this)
                .navigate(EventDetailsScreenDirections
                        .actionEventDetailsScreenToEditEventScreen(userName, eventName));
    }
    // Reads nested structure like:
    // "selectedEvents": { "events": [ ... ] }

    private List<String> readUserList(Map<String, Object> userData, String key) {
        if (userData == null) return new ArrayList<>();
        Object parent = userData.get(key);
        if (!(parent instanceof Map)) return new ArrayList<>();

        Object listObj = ((Map<?, ?>) parent).get("events");
        if (listObj instanceof List) {
            //noinspection unchecked
            return (List<String>) listObj;
        }
        return new ArrayList<>();
    }

    // ------------------------------------------------------------------
    // ACTIONS
    // ------------------------------------------------------------------
    private void joinWaitlist() {
        manager.joinWaitlist(eventName, userName,
                () -> {
                    toast("Joined waitlist");
                    loadEvent();
                },
                e -> toast("Join failed: " + e.getMessage()));
    }

    private void leaveWaitlist() {
        manager.leaveWaitlist(eventName, userName,
                () -> {
                    toast("Left waitlist");
                    loadEvent();
                },
                e -> toast("Leave failed: " + e.getMessage()));
    }

    private void acceptInvite() {
        manager.acceptInvite(eventName, userName,
                () -> {
                    toast("You are now enrolled!");
                    loadEvent();
                },
                e -> toast("Failed to accept invite: " + e.getMessage()));
    }

    private void declineInvite() {
        manager.declineInvite(eventName, userName,
                () -> {
                    toast("You declined the invite.");
                    loadEvent();
                },
                e -> toast("Failed to decline invite: " + e.getMessage()));
    }

    // ------------------------------------------------------------------
    // NAV / UTILS
    // ------------------------------------------------------------------
    private void setupNavButtons() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventDetailsScreenDirections
                                .actionEventDetailsScreenToNotificationScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventDetailsScreenDirections
                                .actionEventDetailsScreenToProfileScreen(userName)));

        binding.btnHome.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventDetailsScreenDirections
                                .actionEventDetailsScreenToHomeScreen(userName)));

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventDetailsScreenDirections.actionEventDetailsScreenToOrganizerEventsScreen(userName)));

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventDetailsScreenDirections.actionEventDetailsScreenToEventHistoryScreen(userName)));

    }

    private void hideAllButtons() {
        binding.btnJoin.setVisibility(View.GONE);
        binding.btnLeave.setVisibility(View.GONE);
        binding.btnAccept.setVisibility(View.GONE);
        binding.btnDecline.setVisibility(View.GONE);
        binding.btnViewWaitList.setVisibility(View.GONE);
        binding.btnViewSelected.setVisibility(View.GONE);
        binding.btnViewCancelled.setVisibility(View.GONE);
        binding.btnViewEnrolled.setVisibility(View.GONE);
        binding.btnBack.setVisibility(View.GONE);
        binding.btnLottery.setVisibility(View.GONE);
    }

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
