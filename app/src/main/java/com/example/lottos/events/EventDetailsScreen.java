package com.example.lottos.events;

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

import androidx.annotation.NonNull;
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
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();

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
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(EventDetailsScreen.this).navigateUp()
        );
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

        // 🔹 FIXED: capacity is top-level, not inside waitList
        String capacity = "No Restriction";

        Object wlCapacityObj = data.get("waitListCapacity");  // ⬅ TOP-LEVEL

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



    private void loadPoster(String url) {
        ImageLoader.load(
                url,
                binding.ivEventPoster,
                com.example.lottos.R.drawable.sample_event
        );
    }


    private String safe(Object o) {
        return o == null ? "" : o.toString();
    }
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
        boolean isSelected   = selectedEvents.contains(eventName);

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

        boolean hasLimit = capacity > 0;
        boolean isFull   = hasLimit && currentWaitSize >= capacity;
        // -------------------------------

        binding.btnBack.setVisibility(View.VISIBLE);

        if (isOpen) {
            binding.btnJoin.setVisibility(View.VISIBLE);

            if (isWaitlisted) {
                // User is already on waitlist -> allow leaving, even if full.
                binding.btnJoin.setEnabled(true);
                binding.btnJoin.setText("Leave Waitlist");
                binding.btnJoin.setOnClickListener(v -> leaveWaitlist());

            } else {
                // User NOT on waitlist yet
                if (isFull) {
                    // Waitlist reached capacity -> block joining
                    binding.btnJoin.setEnabled(false);
                    binding.btnJoin.setText("Waitlist Full");
                    binding.btnJoin.setOnClickListener(null); // no-op
                } else {
                    // Space available -> allow join
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

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

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
