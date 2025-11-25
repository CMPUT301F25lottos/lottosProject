package com.example.lottos.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.account.EditProfileScreen;
import com.example.lottos.account.EditProfileScreenDirections;
import com.example.lottos.account.ProfileScreen;
import com.example.lottos.account.ProfileScreenDirections;
import com.example.lottos.organizer.EditEventScreenArgs;
import com.example.lottos.organizer.EditEventScreenDirections;
import com.example.lottos.EventRepository;
import com.example.lottos.databinding.FragmentEditEventScreenBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * UI for viewing and editing an existing event, including times.
 */
public class EditEventScreen extends Fragment {

    private FragmentEditEventScreenBinding binding;
    private EventRepository repo;
    private OrganizerEventManager manager;
    private String userName;
    private String eventId;

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEditEventScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditEventScreenArgs args = EditEventScreenArgs.fromBundle(getArguments());
        userName = args.getUserName();
        eventId = args.getEventId();

        repo = new EventRepository();
        manager = new OrganizerEventManager();

        loadEventInfo();
        setupNavButtons();

        DateTimePickerHelper timeHelper = new DateTimePickerHelper(requireContext());
        binding.etStartTime.setOnClickListener(v -> timeHelper.showDateTimePicker(binding.etStartTime));
        binding.etEndTime.setOnClickListener(v -> timeHelper.showDateTimePicker(binding.etEndTime));
        binding.etRegisterEndTime.setOnClickListener(v -> timeHelper.showDateTimePicker(binding.etRegisterEndTime));

        binding.btnSave.setOnClickListener(v -> updateEventInfo());
        binding.btnDelete.setOnClickListener(v -> deleteEvent());
        binding.btnCancel.setOnClickListener(v -> goBack());
        binding.btnBack.setOnClickListener(v -> goBack());
    }

    /** Load event data from Firestore and populate UI */
    private void loadEventInfo() {
        DocumentReference eventDoc = repo.getEvent(eventId);
        eventDoc.get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {

                        binding.etEventName.setText(snapshot.getString("eventName"));
                        binding.etEventLocation.setText(snapshot.getString("location"));
                        binding.etDescription.setText(snapshot.getString("description"));

                        Long selectionCap = snapshot.getLong("selectionCap");
                        Long waitListCap = snapshot.getLong("waitListCapacity");

                        if (selectionCap != null) {
                            binding.etCapacity.setText(String.valueOf(selectionCap));
                        }
                        if (waitListCap != null) {
                            binding.etWaitListCapacity.setText(String.valueOf(waitListCap));
                        }

                        Timestamp startTime = snapshot.getTimestamp("startTime");
                        Timestamp endTime = snapshot.getTimestamp("endTime");
                        Timestamp registerEndTime = snapshot.getTimestamp("registerEndTime");

                        if (startTime != null) {
                            LocalDateTime s = startTime.toDate().toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
                            binding.etStartTime.setText(s.format(formatter));
                        }

                        if (endTime != null) {
                            LocalDateTime e = endTime.toDate().toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
                            binding.etEndTime.setText(e.format(formatter));
                        }

                        if (registerEndTime != null) {
                            LocalDateTime r = registerEndTime.toDate().toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
                            binding.etRegisterEndTime.setText(r.format(formatter));
                        }

                    } else {
                        Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load event info.", Toast.LENGTH_SHORT).show()
                );
    }

    /** Update event fields in Firestore */
    private void updateEventInfo() {

        String newEventName = binding.etEventName.getText().toString().trim();
        String location = binding.etEventLocation.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();

        String startStr = binding.etStartTime.getText().toString().trim();
        String endStr = binding.etEndTime.getText().toString().trim();
        String registerEndStr = binding.etRegisterEndTime.getText().toString().trim();

        String selectionCapStr = binding.etCapacity.getText().toString().trim();
        String waitListCapStr = binding.etWaitListCapacity.getText().toString().trim();

        if (newEventName.isEmpty() || location.isEmpty()) {
            Toast.makeText(getContext(), "Event name and location are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer selectionCap = null, waitListCap = null;

        try {
            if (!selectionCapStr.isEmpty()) selectionCap = Integer.parseInt(selectionCapStr);
            if (!waitListCapStr.isEmpty()) waitListCap = Integer.parseInt(waitListCapStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Capacity must be a valid number.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse times
        LocalDateTime startLdt = null, endLdt = null, registerEndLdt = null;

        try {
            if (!startStr.isEmpty()) startLdt = LocalDateTime.parse(startStr, formatter);
            if (!endStr.isEmpty()) endLdt = LocalDateTime.parse(endStr, formatter);
            if (!registerEndStr.isEmpty()) registerEndLdt = LocalDateTime.parse(registerEndStr, formatter);
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Invalid date/time format. Use yyyy-MM-dd HH:mm.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate time order
        if (startLdt != null && endLdt != null && !endLdt.isAfter(startLdt)) {
            Toast.makeText(getContext(),
                    "End time must be after start time.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (registerEndLdt != null && startLdt != null && !startLdt.isAfter(registerEndLdt)) {
            Toast.makeText(getContext(),
                    "Register end time must be before event start time.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("eventName", newEventName);
        updates.put("location", location);
        updates.put("description", description);
        updates.put("selectionCap", selectionCap);
        updates.put("waitListCapacity", waitListCap);

        // --- Corrected code ---
        if (startLdt != null) {
            updates.put("startTime", toTimestamp(startLdt));
        }
        if (endLdt != null) {
            updates.put("endTime", toTimestamp(endLdt));
        }
        if (registerEndLdt != null) {
            updates.put("registerEndTime", toTimestamp(registerEndLdt));
        }


        manager.updateEvent(eventId, updates,
                () -> {
                    Toast.makeText(getContext(), "Event updated successfully!", Toast.LENGTH_SHORT).show();
                    goBack();
                },
                e -> Toast.makeText(getContext(),
                        "Failed to update event: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Helper method to convert a LocalDateTime object to a Firebase Timestamp.
     * @param ldt The LocalDateTime to convert.
     * @return The corresponding Firebase Timestamp.
     */
    private Timestamp toTimestamp(LocalDateTime ldt) {
        if (ldt == null) return null;
        return new Timestamp(java.util.Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
    }


    /** Delete the event document from Firestore */
    private void deleteEvent() {
        manager.deleteEvent(eventId,
                () -> {
                    Toast.makeText(getContext(), "Event deleted.", Toast.LENGTH_SHORT).show();
                    goBack();
                },
                e -> Toast.makeText(getContext(),
                        "Failed to delete event: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void goBack() {
        NavHostFragment.findNavController(this)
                .navigate(EditEventScreenDirections
                        .actionEditEventScreenToOrganizerEventsScreen(userName));
    }

    private void setupNavButtons() {


        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections.actionEditEventScreenToProfileScreen(userName))
        );

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections.actionEditEventScreenToHomeScreen(userName))
        );


        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections.actionEditEventScreenToNotificationScreen(userName))
        );

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections.actionEditEventScreenToOrganizerEventsScreen(userName))
        );

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections.actionEditEventScreenToEventHistoryScreen(userName))
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
