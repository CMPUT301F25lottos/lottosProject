package com.example.lottos.events;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.lottos.databinding.FragmentEventDetailsScreenBinding;
import com.google.firebase.Timestamp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * UI-only Fragment for event details.
 * All Firestore & business logic is delegated to EventDetailsManager.
 * - Shows event details for users.
 * - Shows a "Delete Event" button for admins.
 */
public class EventDetailsScreen extends Fragment {

    private FragmentEventDetailsScreenBinding binding;
    private String eventName;
    private String userName;
    private boolean isAdmin = false;

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

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);

        manager = new EventDetailsManager();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupNavButtons();
        hideAllButtons();


        if (isAdmin) {
            loadBasicEventDataForAdmin();
        } else {
            loadEvent();
        }
    }

    // --- NEW METHOD FOR ADMINS ---
    private void loadBasicEventDataForAdmin() {
        manager.getEventDetails(eventName, eventData -> {
            if (eventData != null) {
                renderEventData(eventData); // Show event details like name, date, etc.

                // Now, show only the admin buttons
                binding.btnBack.setVisibility(View.VISIBLE);
                binding.btnDeleteEvent.setVisibility(View.VISIBLE);
                binding.btnDeleteEvent.setOnClickListener(v -> showDeleteConfirmationDialog());
            } else {
                toast("Error: Event not found.");
            }
        }, e -> toast("Failed to load event details: " + e.getMessage()));
    }


    // ------------------------------------------------------------------
    // LOAD + RENDER (Your existing methods)
    // ------------------------------------------------------------------
    private void loadEvent() {
        manager.loadEventForEntrant(eventName, userName,
                new EventDetailsManager.LoadCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> eventData,
                                          List<String> waitUsers,
                                          Map<String, Object> userData) {
                        renderEventData(eventData);
                        // This method is now ONLY called for regular users, so the admin check is gone.
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

        String capacity = "No Restriction";
        if (waitList != null) {
            Object wlCapacityObj = waitList.get("waitListCapacity");
            if (wlCapacityObj instanceof Number) {
                capacity = String.valueOf(((Number) wlCapacityObj).intValue());
            }
        }
        binding.tvWLSize.setText("Wait List Capacity: " + capacity);
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

        binding.btnBack.setVisibility(View.VISIBLE);

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

        if (!isOpen && isSelected) {
            binding.btnAccept.setVisibility(View.VISIBLE);
            binding.btnDecline.setVisibility(View.VISIBLE);
            binding.btnAccept.setOnClickListener(v -> acceptInvite());
            binding.btnDecline.setOnClickListener(v -> declineInvite());
        }
    }

    private List<String> readUserList(Map<String, Object> userData, String key) {
        if (userData == null) return new ArrayList<>();
        Object parent = userData.get(key);
        if (!(parent instanceof Map)) return new ArrayList<>();
        Object listObj = ((Map<?, ?>) parent).get("events");
        if (listObj instanceof List) {
            return (List<String>) listObj;
        }
        return new ArrayList<>();
    }

    // ------------------------------------------------------------------
    // ACTIONS
    // ------------------------------------------------------------------

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete this event? This action cannot be undone.");

        builder.setPositiveButton("Yes, Delete", (dialog, which) -> {
            binding.btnDeleteEvent.setEnabled(false);
            binding.btnDeleteEvent.setText("Deleting...");

            manager.deleteEvent(eventName,
                    () -> {
                        toast("Event deleted successfully");
                        NavHostFragment.findNavController(EventDetailsScreen.this).navigateUp();
                    },
                    e -> {
                        toast("Error: " + e.getMessage());
                        binding.btnDeleteEvent.setEnabled(true);
                        binding.btnDeleteEvent.setText("Delete Event");
                    });
        });

        builder.setNegativeButton("No, Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

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
        binding.btnAccept.setVisibility(View.GONE);
        binding.btnDecline.setVisibility(View.GONE);
        binding.btnViewWaitList.setVisibility(View.GONE);
        binding.btnViewSelected.setVisibility(View.GONE);
        binding.btnViewCancelled.setVisibility(View.GONE);
        binding.btnViewEnrolled.setVisibility(View.GONE);
        binding.btnBack.setVisibility(View.GONE);
        binding.btnLottery.setVisibility(View.GONE);

        if (binding.btnDeleteEvent != null) {
            binding.btnDeleteEvent.setVisibility(View.GONE);
        }
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
