package com.example.lottos.events;

import android.Manifest;
import android.content.Intent;import android.net.Uri;
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

/**
 * A Fragment that displays the detailed information for a single event.
 * This screen adapts its user interface and available actions based on the user's role (admin vs. regular user)
 * and their status relative to the event (e.g., on the waitlist, selected, not joined).
 * It handles event data fetching, user interactions like joining/leaving a waitlist, accepting/declining invites,
 * and geolocation checks if required by the event.
 * All Firestore & business logic is delegated to EventDetailsManager.
 */
public class EventDetailsScreen extends Fragment {

    private FragmentEventDetailsScreenBinding binding;
    private String eventName;
    private String userName;
    private boolean isAdmin = false;
    private EventDetailsManager manager;

    private boolean isGeolocationRequired = false;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private FusedLocationProviderClient fusedLocationClient;

    /**
     * An ActivityResultLauncher that handles the result of a runtime permission request.
     * It is used here to request the ACCESS_FINE_LOCATION permission.
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    attemptJoinWaitlistWithLocation();
                } else {
                    toast("Location permission is required for this event.");
                }
            });

    private void toast(String msg) {
    /**
     * Displays a short-duration Toast message.
     * @param message The text to display.
     */
    private void toast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initializes the fragment's view, inflates the layout, and retrieves navigation arguments.
     * It also initializes the FusedLocationProviderClient for location services.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The root view of the fragment's layout.
     */
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

    /**
     * Called after the view has been created. This method sets up navigation buttons and determines
     * whether to load the event view for an admin or a regular user.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
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

    /**
     * Loads the essential event details for an administrator. This view is simplified
     * and primarily provides the functionality to delete the event.
     */
    private void loadBasicEventDataForAdmin() {
        binding.btnDeleteEvent.setOnClickListener(v -> showDeleteDialog());

        manager.getEventDetails(eventName, data -> {
            if (data != null) renderEventData(data);
            else toast("Event not found.");
        }, e -> toast("Failed to load event: " + e.getMessage()));
    }

    /**
     * Loads the full event details for a regular user (entrant). This includes the user's
     * status relative to the event (e.g., on waitlist, selected), which is used to
     * configure the UI and available actions.
     */
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
    /**
     * Populates the UI fields with data retrieved from Firestore.
     * @param data A map containing the event's properties.
     */
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
    /**
     * Loads the event poster image from a given URL into the ImageView.
     * Uses a placeholder image if the URL is null or loading fails.
     * @param url The URL of the event poster.
     */
    private void loadPoster(String url) {
        ImageLoader.load(
                url,
                binding.ivEventPoster,
                com.example.lottos.R.drawable.sample_event
        );
    }

    /**
     * Safely converts an object to its string representation, returning an empty string if the object is null.
     * @param o The object to convert.
     * @return The string representation or "" if null.
     */
    private String safe(Object o) {
        return o == null ? "" : o.toString();
    }
    /**
     * Updates the UI buttons and their actions based on the user's relationship with the event
     * (e.g., on waitlist, selected, organizer) and the event's state (open, closed, full).
     * @param eventData The main data for the event.
     * @param waitUsers A list of usernames currently on the waitlist.
     * @param userData  The current user's specific data, including their event lists.
     */
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

        // Show QR code for selected entrant after event is closed
        if (!isOpen && isSelected) {
            binding.imageQRCode.setVisibility(View.VISIBLE);

            String qrContent = eventName + ":" + userName;
            Bitmap qrBitmap = generateQRCode(qrContent);

            if (qrBitmap != null) {
                binding.imageQRCode.setImageBitmap(qrBitmap);
            }
        }
        // Show QR code if the user is enrolled/selected
        if (isOpen) {
            Bitmap qr = generateQRCode(eventName); // generate QR using event name
            binding.ivEventPoster.setImageBitmap(qr); // set QR in the ImageView
            binding.ivEventPoster.setVisibility(View.VISIBLE);
        } else {
            binding.ivEventPoster.setVisibility(View.GONE); // hide if not selected
        }

    }
    /**
     * Navigates to the edit event screen for the current event.
     */
    private void openEditEvent() {
        NavHostFragment.findNavController(this)
                .navigate(EventDetailsScreenDirections
                        .actionEventDetailsScreenToEditEventScreen(userName, eventName));
    }
    /**
     * Helper method to safely extract a list of event strings from the user's data map.
     * Reads a nested structure like: "selectedEvents": { "events": [ ... ] }
     * @param userData The user's data map.
     * @param key      The key for the list to extract (e.g., "selectedEvents").
     * @return A list of event names, or an empty list if not found.
     */
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

    /**
     * Shows a confirmation dialog to the administrator before deleting an event.
     */
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

        builder.setNegativeButton("No, Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Initiates the process of joining the waitlist.
     * If geolocation is required, it first checks for location permissions.
     * Otherwise, it proceeds directly with dummy coordinates.
     */
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

    /**
     * Attempts to get the user's last known location and then calls the method to join the waitlist.
     * This is called after location permissions have been confirmed.
     */
    @SuppressWarnings({"MissingPermission"}) // Permission is checked by caller
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

    /**
     * Performs the final action of joining the waitlist by calling the manager with the user's location.
     * @param lat The user's latitude.
     * @param lon The user's longitude.
     */
    private void performJoinWaitlist(double lat, double lon) {
        manager.joinWaitlist(eventName, userName, lat, lon,
                () -> {
                    toast("Joined waitlist");
                    loadEvent();
                },
                e -> toast("Join failed: " + e.getMessage()));
    }

    /**
     * Handles the action of a user leaving an event's waitlist.
     */
    private void leaveWaitlist() {
        manager.leaveWaitlist(eventName, userName,
                () -> {
                    toast("Left waitlist");
                    loadEvent();
                },
                e -> toast("Leave failed: " + e.getMessage()));
    }

    /**
     * Handles the action of a user accepting an invitation to an event.
     */
    private void acceptInvite() {
        manager.acceptInvite(eventName, userName,
                () -> {
                    toast("Accepted!");
                    loadEvent();
                },
                e -> toast("Failed: " + e.getMessage()));
    }

    /**
     * Handles the action of a user declining an invitation to an event.
     */
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

    /**
     * Sets up the OnClickListener for all navigation buttons in the top and bottom bars.
     */
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

    /**
     * Hides all action buttons in the UI. Useful for resetting state before updating the UI.
     */
    private void hideAllButtons() {
        binding.btnJoin.setVisibility(View.GONE);
        binding.btnAccept.setVisibility(View.GONE);
        binding.btnDecline.setVisibility(View.GONE);
        binding.btnViewWaitList.setVisibility(View.GONE);
        binding.btnViewSelected.setVisibility(View.GONE);
        binding.btnViewCancelled.setVisibility(View.GONE);
        binding.btnViewEnrolled.setVisibility(View.GONE);

        if (binding.btnDeleteEvent != null) {
            binding.btnDeleteEvent.setVisibility(View.GONE);
        }
    }

    /**
     * Cleans up resources when the view is destroyed.
     * This includes nullifying the view binding and shutting down the executor service.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        imageExecutor.shutdown();
        binding = null;
    }
}
