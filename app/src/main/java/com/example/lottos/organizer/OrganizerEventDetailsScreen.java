package com.example.lottos.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

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
    public void onViewCreated(@NonNull View view,
                              Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupNavButtons();
        loadEvent();
    }

    private void loadEvent() {
        manager.loadEvent(eventId, new OrganizerEventDetailsManager.LoadCallback() {
            @Override
            public void onSuccess(Map<String, Object> eventData,
                                  List<String> waitlistUsers,
                                  List<String> selectedUsers,
                                  List<String> notSelectedUsers,
                                  List<String> enrolledUsers,
                                  List<String> cancelledUsers) {

                renderHeader(eventData);

                binding.tvWaitlistHeader.setText("Waitlist (" + waitlistUsers.size() + ")");
                binding.tvSelectedHeader.setText("Selected (" + selectedUsers.size() + ")");
                binding.tvNotSelectedHeader.setText("Not Selected (" + notSelectedUsers.size() + ")");
                binding.tvEnrolledHeader.setText("Enrolled (" + enrolledUsers.size() + ")");
                binding.tvCancelledHeader.setText("Cancelled (" + cancelledUsers.size() + ")");

                renderListSection(binding.containerWaitlist, binding.tvWaitlistEmpty, waitlistUsers);
                renderListSection(binding.containerSelected, binding.tvSelectedEmpty, selectedUsers);
                renderListSection(binding.containerNotSelected, binding.tvNotSelectedEmpty, notSelectedUsers);
                renderListSection(binding.containerEnrolled, binding.tvEnrolledEmpty, enrolledUsers);
                renderListSection(binding.containerCancelled, binding.tvCancelledEmpty, cancelledUsers);

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

        Object startTimeObj = data.get("startTime");
        Object endTimeObj   = data.get("endTime");

        String dateText = "Date: N/A";
        String timeText = "Time: N/A";

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (startTimeObj instanceof Timestamp) {
            Timestamp tsStart = (Timestamp) startTimeObj;
            Date dStart = tsStart.toDate();
            dateText = "Date: " + dateFmt.format(dStart);
            timeText = "Time: " + timeFmt.format(dStart);
        }

        if (endTimeObj instanceof Timestamp) {
            Timestamp tsEnd = (Timestamp) endTimeObj;
            Date dEnd = tsEnd.toDate();
            timeText = timeText + " - " + timeFmt.format(dEnd);
        }

        binding.tvEventDate.setText(dateText);
        binding.tvEventTime.setText(timeText);
    }

    /**
     * Populates one section:
     * - If users list is empty: show the "empty" label
     * - If not empty: hide "empty" label and show "• username" rows
     */
    private void renderListSection(LinearLayout container,
                                   TextView emptyView,
                                   List<String> users) {

        container.removeAllViews();

        if (users == null || users.isEmpty()) {
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
            return;
        }

        if (emptyView != null) emptyView.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (String u : users) {
            TextView tv = (TextView) inflater.inflate(
                    android.R.layout.simple_list_item_1,
                    container,
                    false
            );
            tv.setText("• " + u);
            container.addView(tv);
        }
    }

    private String safe(Object o) {
        return o == null ? "" : o.toString();
    }

    private void setupNavButtons() {
        // Home (back/home icon)
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventDetailsScreenDirections
                                .actionOrganizerEventDetailsScreenToHomeScreen(userName)));

        // Open Events (back to organizer events list)
        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventDetailsScreenDirections
                                .actionOrganizerEventDetailsScreenToOrganizerEventsScreen(userName)));

        // Notifications
        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventDetailsScreenDirections
                                .actionOrganizerEventDetailsScreenToNotificationScreen(userName)));

        // Event History
        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventDetailsScreenDirections
                                .actionOrganizerEventDetailsScreenToEventHistoryScreen(userName)));

        // Profile
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
