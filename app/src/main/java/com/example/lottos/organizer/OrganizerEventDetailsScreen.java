package com.example.lottos.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.TimeUtils;
import com.example.lottos.databinding.FragmentOrganizerEventDetailsScreenBinding;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * UI-only Fragment for organizer-specific event details.
 * All Firestore / business logic lives in OrganizerEventDetailsManager.
 */
public class OrganizerEventDetailsScreen extends Fragment {
    private FragmentOrganizerEventDetailsScreenBinding binding;
    private OrganizerEventDetailsManager manager;
    private String userName;
    private String eventId;

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
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadEvent();
        setupNavButtons();
    }

    private void loadEvent() {
        manager.loadEvent(eventId, new OrganizerEventDetailsManager.LoadCallback() {
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

            @Override
            public void onError(Exception e) {
                toast("Failed to load event: " + e.getMessage());
            }
        });
    }

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

    private void updateUI(Map<String, Object> eventData, List<String> waitUsers) {

        if (eventData == null) return;

        boolean isOpen     = Boolean.TRUE.equals(eventData.get("IsOpen"));
        boolean hasRunLottery = Boolean.TRUE.equals(eventData.get("IsLottery"));
        String organizer   = safe(eventData.get("organizer"));
        boolean isOrganizer = organizer.equalsIgnoreCase(userName);

        binding.btnLottery.setVisibility(View.GONE);
        binding.btnLottery.setOnClickListener(null);

        if (isOrganizer && !hasRunLottery && waitUsers != null && !waitUsers.isEmpty()) {
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

    private String safe(Object o) {
        return o == null ? "" : o.toString();
    }
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
    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
