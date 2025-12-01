package com.example.lottos.events;

import android.content.Intent;import android.net.Uri;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.lottos.ImageLoader;
import com.example.lottos.TimeUtils;
import com.example.lottos.databinding.FragmentEventDetailsScreenBinding;
import com.google.firebase.Timestamp;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

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
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private boolean isGeolocationRequired = false; // Story 2 state read from event
    private FusedLocationProviderClient fusedLocationClient;

    /**
     * An ActivityResultLauncher that handles the result of a runtime permission request.
     * It is used here to request the ACCESS_FINE_LOCATION permission.
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, now attempt to get location
                    attemptJoinWaitlistWithLocation();
                } else {
                    // Permission denied
                    toast("Location permission is required for this event.");
                }
            });

    /**
     * Displays a short-duration Toast message.
     * @param message The text to display.
     */
    private void toast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentEventDetailsScreenBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            EventDetailsScreenArgs args = EventDetailsScreenArgs.fromBundle(getArguments());
            eventName = args.getEventName();
            userName = args.getUserName();
        }

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);

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
        binding.btnDeleteEvent.setOnClickListener(v -> showDeleteConfirmationDialog());

        manager.getEventDetails(eventName, eventData -> {
            if (eventData != null) {
                renderEventData(eventData);
            } else {
                toast("Error: Event not found.");
            }
        }, e -> toast("Failed to load event details: " + e.getMessage()));
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
                    public void onSuccess(Map<String, Object> eventData, List<String> waitUsers, Map<String, Object> userData) {
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

        String posterUrl = (String) data.get("posterUrl");
        loadPoster(posterUrl);

        binding.tvEventName.setText(safe(data.get("eventName")));
        binding.tvLocation.setText(safe(data.get("location")));

        binding.tvStartTime.setText(TimeUtils.formatEventTime(data.get("startTime")));
        binding.tvEndTime.setText(" ~ " + TimeUtils.formatEventTime(data.get("endTime")));
        binding.tvWLCloseDateTime.setText(
                "Register End Time: " + TimeUtils.formatEventTime(data.get("registerEndTime"))
        );

        Map<String, Object> waitList = (Map<String, Object>) data.get("waitList");
        if (waitList != null) {
            List<?> users = (List<?>) waitList.get("users");
            int userCount = (users != null) ? users.size() : 0;
            binding.tvWLCount.setText("Number of Entrants on WaitList: " + userCount);
        } else {
            binding.tvWLCount.setText("Number of Entrants on WaitList: 0");
        }

        binding.tvDescription.setText(safe(data.get("description")));
        binding.tvCapacity.setText("Event Capacity: " + safe(data.get("selectionCap")));

        Object geoReqObj = data.get("geolocationRequired");
        isGeolocationRequired = Boolean.TRUE.equals(geoReqObj);

        String capacity = "No Restriction";

        Object wlCapacityObj = data.get("waitListCapacity");

        if (wlCapacityObj instanceof Number) {
            capacity = String.valueOf(((Number) wlCapacityObj).intValue());
        } else if (wlCapacityObj instanceof String) {
            try {
                capacity = String.valueOf(Integer.parseInt((String) wlCapacityObj));
            } catch (NumberFormatException ignored) {
                // leave as "No Restriction"
            }
        }

        binding.tvWLSize.setText("Wait List Capacity: " + capacity);
    }


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
        boolean isLottery = Boolean.TRUE.equals(eventData.get("IsLottery"));
        String organizer = safe(eventData.get("organizer"));
        boolean isOrganizer = organizer.equalsIgnoreCase(userName);

        List<String> selectedEvents   = readUserList(userData, "selectedEvents");
        List<String> waitlistedEvents = readUserList(userData, "waitListedEvents");
        boolean isWaitlisted = waitlistedEvents.contains(eventName);
        boolean isSelected = selectedEvents.contains(eventName);

        // boolean isSelected   = selectedEvents.contains(eventName);

        // Show QR code if user is selected/enrolled
        if (isSelected) {
            Bitmap qr = generateQRCode(userName + "_" + eventName);
            if (qr != null) {
                binding.ivEventPoster.setImageBitmap(qr);
            }
        }


        // --- WAITLIST CAPACITY LOGIC ---
        int currentWaitSize = (waitUsers != null) ? waitUsers.size() : 0;

        int capacity = -1;  // -1 = no limit
        Object capObj = eventData.get("waitListCapacity");  // top-level field
        if (capObj instanceof Number) {
            capacity = ((Number) capObj).intValue();
        }

        // Entrant join/leave

        boolean hasLimit = capacity > 0;
        boolean isFull   = hasLimit && currentWaitSize >= capacity;



        if (isOpen) {
            binding.btnJoin.setVisibility(View.VISIBLE);

            if (isWaitlisted) {
                binding.btnJoin.setEnabled(true);
                binding.btnJoin.setText("Leave Waitlist");
                binding.btnJoin.setOnClickListener(v -> leaveWaitlist());

            } else {
                if (isFull) {
                    binding.btnJoin.setEnabled(false);
                    binding.btnJoin.setText("Waitlist Full");
                    binding.btnJoin.setOnClickListener(null); // no-op
                } else {
                    binding.btnJoin.setEnabled(true);
                    binding.btnJoin.setText("Join Waitlist");
                    binding.btnJoin.setOnClickListener(v -> joinWaitlist());
                }
            }
        } else {
            binding.btnJoin.setVisibility(View.GONE);
        }

        if (!isOpen && isSelected) {
            binding.btnAccept.setVisibility(View.VISIBLE);
            binding.btnDecline.setVisibility(View.VISIBLE);

            binding.btnAccept.setOnClickListener(v -> acceptInvite());
            binding.btnDecline.setOnClickListener(v -> declineInvite());

        } else {
            binding.btnAccept.setVisibility(View.GONE);
            binding.btnDecline.setVisibility(View.GONE);
        }

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
            // Check if permission is already granted
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                attemptJoinWaitlistWithLocation();
            } else {
                // Request permission using the launcher defined above
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } else {
            // If location is NOT required, use dummy coordinates (0, 0)
            performJoinWaitlist(0.0, 0.0);
        }
    }

    /**
     * Attempts to get the user's last known location and then calls the method to join the waitlist.
     * This is called after location permissions have been confirmed.
     */
    @SuppressWarnings({"MissingPermission"}) // Permission is checked by caller
    private void attemptJoinWaitlistWithLocation() {
        // Attempt to get the user's last known location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        performJoinWaitlist(location.getLatitude(), location.getLongitude());
                    } else {
                        toast("Could not get location. Try again.");
                    }
                })
                .addOnFailureListener(e -> {
                    toast("Location service error: " + e.getMessage());
                });
    }

    /**
     * Performs the final action of joining the waitlist by calling the manager with the user's location.
     * @param lat The user's latitude.
     * @param lon The user's longitude.
     */
    private void performJoinWaitlist(double lat, double lon) {
        // Call the updated manager method with location
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
                    toast("You are now enrolled!");
                    loadEvent();
                },
                e -> toast("Failed to accept invite: " + e.getMessage()));
    }

    /**
     * Handles the action of a user declining an invitation to an event.
     */
    private void declineInvite() {
        manager.declineInvite(eventName, userName,
                () -> {
                    toast("You declined the invite.");
                    loadEvent();
                },
                e -> toast("Failed to decline invite: " + e.getMessage()));
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
        binding = null;
        imageExecutor.shutdown();
    }

    private Bitmap generateQRCode(String text) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    512, 512
            );
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

}
