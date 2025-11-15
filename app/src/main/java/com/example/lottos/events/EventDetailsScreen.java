package com.example.lottos.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
        binding.tvLocation.setText("Location: " + safe(data.get("location")));
        binding.tvDescription.setText("Description: " + safe(data.get("description")));
        binding.tvCapacity.setText("Capacity: " + safe(data.get("selectionCap")));
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
