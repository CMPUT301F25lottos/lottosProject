package com.example.lottos.events;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.ImageLoader;
import com.example.lottos.TimeUtils;
import com.example.lottos.databinding.FragmentEventDetailsScreenBinding;
import com.example.lottos.events.QRCodeGenerator;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventDetailsScreen extends Fragment {

    private FragmentEventDetailsScreenBinding binding;
    private String eventName;
    private String userName;
    private boolean isAdmin = false;
    private EventDetailsManager manager;

    private boolean isGeolocationRequired = false;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    attemptJoinWaitlistWithLocation();
                } else {
                    toast("Location permission is required for this event.");
                }
            });

    private void toast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentEventDetailsScreenBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            EventDetailsScreenArgs args = EventDetailsScreenArgs.fromBundle(getArguments());
            eventName = args.getEventName();
            userName = args.getUserName();
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        isAdmin = prefs.getBoolean("isAdmin", false);

        manager = new EventDetailsManager();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupNavButtons();

        if (isAdmin) {
            binding.llAdminButtons.setVisibility(View.VISIBLE);
            binding.llButtons.setVisibility(View.GONE);
            loadBasicEventDataForAdmin();
        } else {
            binding.llButtons.setVisibility(View.VISIBLE);
            binding.llAdminButtons.setVisibility(View.GONE);
            loadEvent();
        }
    }

    /** ─────────────────────────────────────────────────────────────
     *  ADMIN LOAD
     *  ───────────────────────────────────────────────────────────── */
    private void loadBasicEventDataForAdmin() {
        binding.btnDeleteEvent.setOnClickListener(v -> showDeleteDialog());

        manager.getEventDetails(eventName, data -> {
            if (data != null) renderEventData(data);
            else toast("Event not found.");
        }, e -> toast("Failed to load event: " + e.getMessage()));
    }

    /** ─────────────────────────────────────────────────────────────
     *  ENTRANT LOAD
     *  ───────────────────────────────────────────────────────────── */
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

    /** ─────────────────────────────────────────────────────────────
     *  UI RENDER – TEXT + POSTER ONLY
     *  ───────────────────────────────────────────────────────────── */
    private void renderEventData(Map<String, Object> data) {

        ImageLoader.load(
                (String) data.get("posterUrl"),
                binding.ivEventPoster,
                com.example.lottos.R.drawable.sample_event
        );

        binding.tvEventName.setText(safe(data.get("eventName")));
        binding.tvLocation.setText(safe(data.get("location")));
        binding.tvStartTime.setText(TimeUtils.formatEventTime(data.get("startTime")));
        binding.tvEndTime.setText(" ~ " + TimeUtils.formatEventTime(data.get("endTime")));

        binding.tvWLCloseDateTime.setText(
                "Register End Time: " + TimeUtils.formatEventTime(data.get("registerEndTime"))
        );

        Object waitList = data.get("waitList");
        if (waitList instanceof Map && ((Map<?, ?>) waitList).get("users") instanceof List) {
            binding.tvWLCount.setText("Number of Entrants on WaitList: " +
                    ((List<?>) ((Map<?, ?>) waitList).get("users")).size());
        }

        binding.tvDescription.setText(safe(data.get("description")));
        binding.tvCapacity.setText("Event Capacity: " + safe(data.get("selectionCap")));

        Object geoReq = data.get("geolocationRequired");
        isGeolocationRequired = Boolean.TRUE.equals(geoReq);

        Object cap = data.get("waitListCapacity");
        binding.tvWLSize.setText("Wait List Capacity: " +
                (cap instanceof Number ? String.valueOf(cap) : "No Restriction"));
    }

    private String safe(Object o) { return o == null ? "" : o.toString(); }

    private List<String> readUserList(Map<String, Object> userData, String key) {
        if (userData == null) return new ArrayList<>();
        Object parent = userData.get(key);
        if (!(parent instanceof Map)) return new ArrayList<>();
        Object list = ((Map<?, ?>) parent).get("events");
        if (list instanceof List) return (List<String>) list;
        return new ArrayList<>();
    }

    /** ─────────────────────────────────────────────────────────────
     *  MAIN UI LOGIC
     *  Handles: Join/Leave | Accept/Decline | Show QR
     *  ───────────────────────────────────────────────────────────── */
    private void updateUI(Map<String, Object> eventData,
                          List<String> waitUsers,
                          Map<String, Object> userData) {

        boolean isOpen = Boolean.TRUE.equals(eventData.get("IsOpen"));
        List<String> selected = readUserList(userData, "selectedEvents");
        List<String> waitlisted = readUserList(userData, "waitListedEvents");

        boolean isSelected = selected.contains(eventName);
        boolean isWaitlisted = waitlisted.contains(eventName);

        int currentWait = waitUsers != null ? waitUsers.size() : 0;
        int capacity = (eventData.get("waitListCapacity") instanceof Number)
                ? ((Number) eventData.get("waitListCapacity")).intValue()
                : -1;

        boolean isFull = capacity > 0 && currentWait >= capacity;


        /** ───────────────────────────────────────────────
         *  ALWAYS SHOW QR CODE AT BOTTOM OF PAGE
         *  ─────────────────────────────────────────────── */
        String qrContent = userName + "_" + eventName;
        Bitmap qr = QRCodeGenerator.generate(qrContent, 512);

        binding.imageQRCode.setVisibility(View.VISIBLE);
        binding.imageQRCode.setImageBitmap(qr);


        /** ───────────────────────────────────────────────
         *  WAITLIST JOIN / LEAVE BUTTONS
         *  ─────────────────────────────────────────────── */
        if (isOpen) {
            binding.btnJoin.setVisibility(View.VISIBLE);

            if (isWaitlisted) {
                binding.btnJoin.setEnabled(true);
                binding.btnJoin.setText("Leave Waitlist");
                binding.btnJoin.setOnClickListener(v -> leaveWaitlist());

            } else if (isFull) {
                binding.btnJoin.setEnabled(false);
                binding.btnJoin.setText("Waitlist Full");

            } else {
                binding.btnJoin.setEnabled(true);
                binding.btnJoin.setText("Join Waitlist");
                binding.btnJoin.setOnClickListener(v -> joinWaitlist());
            }

        } else {
            binding.btnJoin.setVisibility(View.GONE);
        }

        /** ───────────────────────────────────────────────
         *  ACCEPT / DECLINE UI
         *  ─────────────────────────────────────────────── */
        if (!isOpen && isSelected) {
            binding.btnAccept.setVisibility(View.VISIBLE);
            binding.btnDecline.setVisibility(View.VISIBLE);

            binding.btnAccept.setOnClickListener(v -> acceptInvite());
            binding.btnDecline.setOnClickListener(v -> declineInvite());
        } else {
            binding.btnAccept.setVisibility(View.GONE);
            binding.btnDecline.setVisibility(View.GONE);
        }
    }


    /** ─────────────────────────────────────────────────────────────
     *  JOIN / LEAVE / ACCEPT / DECLINE
     *  ───────────────────────────────────────────────────────────── */
    private void joinWaitlist() {
        if (isGeolocationRequired) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                attemptJoinWaitlistWithLocation();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } else {
            performJoinWaitlist(0.0, 0.0);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void attemptJoinWaitlistWithLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        performJoinWaitlist(location.getLatitude(), location.getLongitude());
                    } else {
                        toast("Could not get location. Try again.");
                    }
                })
                .addOnFailureListener(e -> toast("Location error: " + e.getMessage()));
    }

    private void performJoinWaitlist(double lat, double lon) {
        manager.joinWaitlist(eventName, userName, lat, lon,
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
                    toast("Accepted!");
                    loadEvent();
                },
                e -> toast("Failed: " + e.getMessage()));
    }

    private void declineInvite() {
        manager.declineInvite(eventName, userName,
                () -> {
                    toast("Declined invite.");
                    loadEvent();
                },
                e -> toast("Failed: " + e.getMessage()));
    }

    /** ─────────────────────────────────────────────────────────────
     *  DELETE CONFIRMATION
     *  ───────────────────────────────────────────────────────────── */
    private void showDeleteDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (d, w) -> {
                    binding.btnDeleteEvent.setEnabled(false);
                    binding.btnDeleteEvent.setText("Deleting...");

                    manager.deleteEvent(eventName,
                            () -> {
                                toast("Event deleted");
                                NavHostFragment.findNavController(this).navigateUp();
                            },
                            e -> {
                                toast("Error: " + e.getMessage());
                                binding.btnDeleteEvent.setEnabled(true);
                                binding.btnDeleteEvent.setText("Delete Event");
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** ─────────────────────────────────────────────────────────────
     *  NAVIGATION BUTTONS
     *  ───────────────────────────────────────────────────────────── */
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
                        .navigate(EventDetailsScreenDirections
                                .actionEventDetailsScreenToOrganizerEventsScreen(userName)));

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventDetailsScreenDirections
                                .actionEventDetailsScreenToEventHistoryScreen(userName)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        imageExecutor.shutdown();
        binding = null;
    }
}
