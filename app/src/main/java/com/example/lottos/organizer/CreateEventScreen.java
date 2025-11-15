package com.example.lottos.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.organizer.CreateEventScreenArgs;
import com.example.lottos.organizer.CreateEventScreenDirections;
import com.example.lottos.entities.Event;
import com.example.lottos.databinding.FragmentCreateEventScreenBinding;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * UI for creating a new event.
 * Firestore operations handled by OrganizerEventManager.
 */
public class CreateEventScreen extends Fragment {

    private FragmentCreateEventScreenBinding binding;
    private final OrganizerEventManager manager = new OrganizerEventManager();
    private String userName;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentCreateEventScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = CreateEventScreenArgs.fromBundle(getArguments()).getUserName();

        DateTimePickerHelper timeHelper = new DateTimePickerHelper(requireContext());
        binding.etRegisterEndTime.setOnClickListener(v -> timeHelper.showDateTimePicker(binding.etRegisterEndTime));
        binding.etStartTime.setOnClickListener(v -> timeHelper.showDateTimePicker(binding.etStartTime));
        binding.etEndTime.setOnClickListener(v -> timeHelper.showDateTimePicker(binding.etEndTime));

        binding.btnCancel.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToOrganizerEventsScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToNotificationScreen(userName)));

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToHomeScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToProfileScreen(userName)));

        binding.btnCreateEvent.setOnClickListener(v -> handleCreateEvent());
    }

    private void handleCreateEvent() {

        String eventName = binding.etEventName.getText().toString().trim();
        String location = binding.etEventLocation.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();

        String registerEndTime = binding.etRegisterEndTime.getText().toString().trim();
        String startTime = binding.etStartTime.getText().toString().trim();
        String endTime = binding.etEndTime.getText().toString().trim();

        String capStr = binding.etCapacity.getText().toString().trim();
        String wlCapStr = binding.etWaitListCapacity.getText().toString().trim();

        if (eventName.isEmpty() || location.isEmpty() ||
                startTime.isEmpty() || endTime.isEmpty() ||
                capStr.isEmpty() || registerEndTime.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity;
        Integer waitListCapacity = null;
        try {
            capacity = Integer.parseInt(capStr);
            if (!wlCapStr.isEmpty()) {
                waitListCapacity = Integer.parseInt(wlCapStr);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(),
                    "Enter valid numbers for capacities.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (capacity <= 0) {
            Toast.makeText(requireContext(),
                    "Capacity must be greater than 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime startLdt, endLdt, registerEndLdt;
        try {
            startLdt = LocalDateTime.parse(startTime, formatter);
            endLdt = LocalDateTime.parse(endTime, formatter);
            registerEndLdt = LocalDateTime.parse(registerEndTime, formatter);
        } catch (DateTimeParseException e) {
            Toast.makeText(requireContext(),
                    "Invalid date/time format. Use yyyy-MM-dd HH:mm", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!endLdt.isAfter(startLdt)) {
            Toast.makeText(requireContext(),
                    "End time must be after start time.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!startLdt.isAfter(registerEndLdt)) {
            Toast.makeText(requireContext(),
                    "Register end time must be before event start time.", Toast.LENGTH_SHORT).show();
            return;
        }

        Event event = new Event(eventName, userName, startLdt, endLdt, desc, location, capacity, registerEndLdt);

        manager.createEvent(event, registerEndLdt, waitListCapacity,
                () -> {
                    Toast.makeText(requireContext(),
                            "Event created and linked to your organized events!",
                            Toast.LENGTH_SHORT).show();

                    NavHostFragment.findNavController(CreateEventScreen.this)
                            .navigate(CreateEventScreenDirections
                                    .actionCreateEventScreenToOrganizerEventsScreen(userName));
                },
                e -> Toast.makeText(requireContext(),
                        "Failed to create event: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
